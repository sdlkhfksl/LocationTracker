"""Config flow for ha_loca_device."""
from __future__ import annotations

import re
from typing import Any

import voluptuous as vol

from homeassistant import config_entries
from homeassistant.config_entries import ConfigFlowResult
from homeassistant.const import CONF_NAME
from homeassistant.core import callback
from homeassistant.helpers import config_validation as cv
from homeassistant.helpers.network import get_url

from .const import (
    CONF_UPDATE_INTERVAL,
    CONF_WEBHOOK_ID,
    DEFAULT_UPDATE_INTERVAL,
    DOMAIN,
    MAX_UPDATE_INTERVAL,
    MIN_UPDATE_INTERVAL,
)
from .coordinator import clamp_interval

_WEBHOOK_ID_RE = re.compile(r"^[A-Za-z0-9_-]{8,128}$")


def _webhook_url(hass, webhook_id: str) -> str:
    try:
        base = get_url(hass, prefer_external=True, allow_cloud=True)
    except Exception:
        base = "https://YOUR_HA_URL"
    return f"{base.rstrip('/')}/api/webhook/{webhook_id}"


def _normalize_webhook_id(raw: str) -> str | None:
    value = (raw or "").strip()
    if "/api/webhook/" in value:
        value = value.rsplit("/api/webhook/", 1)[-1].strip("/")
    value = value.split("?", 1)[0].strip("/")
    if not _WEBHOOK_ID_RE.match(value):
        return None
    return value


class LocaDeviceConfigFlow(config_entries.ConfigFlow, domain=DOMAIN):
    VERSION = 1

    async def async_step_user(
        self, user_input: dict[str, Any] | None = None
    ) -> ConfigFlowResult:
        errors: dict[str, str] = {}

        if user_input is not None:
            name = user_input[CONF_NAME].strip()
            webhook_id = _normalize_webhook_id(user_input[CONF_WEBHOOK_ID])
            interval = clamp_interval(
                user_input.get(CONF_UPDATE_INTERVAL, DEFAULT_UPDATE_INTERVAL)
            )
            if not name:
                errors["base"] = "invalid_name"
            elif webhook_id is None:
                errors[CONF_WEBHOOK_ID] = "invalid_webhook_id"
            else:
                await self.async_set_unique_id(webhook_id)
                self._abort_if_unique_id_configured()
                return self.async_create_entry(
                    title=name,
                    data={
                        CONF_NAME: name,
                        CONF_WEBHOOK_ID: webhook_id,
                    },
                    options={CONF_UPDATE_INTERVAL: interval},
                    description_placeholders={
                        "webhook_url": _webhook_url(self.hass, webhook_id),
                    },
                )

        return self.async_show_form(
            step_id="user",
            data_schema=vol.Schema(
                {
                    vol.Required(CONF_NAME): cv.string,
                    vol.Required(CONF_WEBHOOK_ID): cv.string,
                    vol.Required(
                        CONF_UPDATE_INTERVAL, default=DEFAULT_UPDATE_INTERVAL
                    ): vol.All(
                        vol.Coerce(int),
                        vol.Range(min=MIN_UPDATE_INTERVAL, max=MAX_UPDATE_INTERVAL),
                    ),
                }
            ),
            errors=errors,
        )

    @staticmethod
    @callback
    def async_get_options_flow(
        config_entry: config_entries.ConfigEntry,
    ) -> config_entries.OptionsFlow:
        return LocaDeviceOptionsFlow()


class LocaDeviceOptionsFlow(config_entries.OptionsFlow):
    async def async_step_init(
        self, user_input: dict[str, Any] | None = None
    ) -> ConfigFlowResult:
        webhook_id = self.config_entry.data[CONF_WEBHOOK_ID]
        url = _webhook_url(self.hass, webhook_id)
        if user_input is not None:
            return self.async_create_entry(
                title="",
                data={
                    CONF_UPDATE_INTERVAL: clamp_interval(
                        user_input[CONF_UPDATE_INTERVAL]
                    ),
                },
            )

        current = clamp_interval(
            self.config_entry.options.get(
                CONF_UPDATE_INTERVAL,
                self.config_entry.data.get(
                    CONF_UPDATE_INTERVAL, DEFAULT_UPDATE_INTERVAL
                ),
            )
        )
        return self.async_show_form(
            step_id="init",
            data_schema=vol.Schema(
                {
                    vol.Required(CONF_UPDATE_INTERVAL, default=current): vol.All(
                        vol.Coerce(int),
                        vol.Range(min=MIN_UPDATE_INTERVAL, max=MAX_UPDATE_INTERVAL),
                    ),
                }
            ),
            description_placeholders={
                "webhook_url": url,
                "webhook_id": webhook_id,
            },
        )
