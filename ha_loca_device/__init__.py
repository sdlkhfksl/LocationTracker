"""The ha_loca_device integration — App pushes to HA webhook."""
from __future__ import annotations

import logging
from typing import Any

from aiohttp import web

from homeassistant.components import webhook
from homeassistant.config_entries import ConfigEntry
from homeassistant.const import CONF_NAME
from homeassistant.core import HomeAssistant
from homeassistant.helpers.typing import ConfigType

from .const import CONF_WEBHOOK_ID, DOMAIN, PLATFORMS
from .coordinator import LocaDeviceCoordinator

_LOGGER = logging.getLogger(__name__)


async def async_setup(hass: HomeAssistant, config: ConfigType) -> bool:
    hass.data.setdefault(DOMAIN, {})
    return True


async def async_setup_entry(hass: HomeAssistant, entry: ConfigEntry) -> bool:
    hass.data.setdefault(DOMAIN, {})

    coordinator = LocaDeviceCoordinator(hass, entry)
    await coordinator.async_load()
    hass.data[DOMAIN][entry.entry_id] = coordinator

    webhook_id = entry.data[CONF_WEBHOOK_ID]
    webhook.async_register(
        hass,
        DOMAIN,
        f"ha_loca_device {entry.data[CONF_NAME]}",
        webhook_id,
        _make_webhook_handler(entry.entry_id),
    )

    await hass.config_entries.async_forward_entry_setups(entry, PLATFORMS)
    return True


async def async_unload_entry(hass: HomeAssistant, entry: ConfigEntry) -> bool:
    webhook.async_unregister(hass, entry.data[CONF_WEBHOOK_ID])
    unload_ok = await hass.config_entries.async_unload_platforms(entry, PLATFORMS)
    if unload_ok:
        hass.data[DOMAIN].pop(entry.entry_id, None)
    return unload_ok


def _make_webhook_handler(entry_id: str):
    async def handle_webhook(
        hass: HomeAssistant,
        webhook_id: str,
        request: web.Request,
    ) -> web.Response:
        coordinator: LocaDeviceCoordinator | None = hass.data.get(DOMAIN, {}).get(
            entry_id
        )
        if coordinator is None:
            return web.json_response({"ok": False, "error": "not_ready"}, status=503)

        try:
            payload: dict[str, Any] = await request.json()
        except Exception:
            return web.json_response({"ok": False, "error": "invalid_json"}, status=400)

        if not isinstance(payload, dict):
            return web.json_response({"ok": False, "error": "invalid_json"}, status=400)

        ok = await coordinator.async_update_from_payload(payload)
        if not ok:
            return web.json_response(
                {"ok": False, "error": "invalid_location"}, status=400
            )
        return web.json_response(
            {
                "ok": True,
                "update_interval": coordinator.update_interval_seconds,
            }
        )

    return handle_webhook
