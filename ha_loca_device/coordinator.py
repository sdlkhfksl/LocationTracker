"""Location state for ha_loca_device (push via webhook)."""
from __future__ import annotations

from datetime import datetime, timezone
import logging
from typing import Any

from homeassistant.config_entries import ConfigEntry
from homeassistant.const import CONF_NAME
from homeassistant.core import HomeAssistant
from homeassistant.helpers.dispatcher import async_dispatcher_send
from homeassistant.helpers.storage import Store
from homeassistant.util import dt as dt_util

from .const import (
    ATTR_ALTITUDE,
    ATTR_BATTERY,
    ATTR_BEARING,
    ATTR_GPS_ACCURACY,
    ATTR_IMMEDIATE_REPORT,
    ATTR_LATITUDE,
    ATTR_LONGITUDE,
    ATTR_POWER_SAVE_MODE,
    ATTR_PROVIDER,
    ATTR_SCREEN_OFF,
    ATTR_SPEED,
    ATTR_TIMESTAMP,
    CONF_UPDATE_INTERVAL,
    DEFAULT_UPDATE_INTERVAL,
    MAX_UPDATE_INTERVAL,
    MIN_UPDATE_INTERVAL,
    SIGNAL_LOCATION_UPDATE,
    STORAGE_KEY,
    STORAGE_VERSION,
)

_LOGGER = logging.getLogger(__name__)


def _as_float(value: Any) -> float | None:
    if value is None or value == "":
        return None
    try:
        return float(value)
    except (TypeError, ValueError):
        return None


def _as_int(value: Any) -> int | None:
    num = _as_float(value)
    if num is None:
        return None
    return int(num)


def _as_bool(value: Any) -> bool | None:
    if value is None:
        return None
    if isinstance(value, bool):
        return value
    if isinstance(value, (int, float)):
        return bool(value)
    if isinstance(value, str):
        return value.lower() in ("1", "true", "yes")
    return None


def clamp_interval(value: Any) -> int:
    try:
        seconds = int(value)
    except (TypeError, ValueError):
        return DEFAULT_UPDATE_INTERVAL
    return max(MIN_UPDATE_INTERVAL, min(MAX_UPDATE_INTERVAL, seconds))


class LocaDeviceCoordinator:
    """Hold last known location pushed by the phone."""

    def __init__(self, hass: HomeAssistant, entry: ConfigEntry) -> None:
        self.hass = hass
        self.entry = entry
        self.device_name = entry.data[CONF_NAME]
        self.entry_id = entry.entry_id
        self._store = Store(hass, STORAGE_VERSION, f"{STORAGE_KEY}.{entry.entry_id}")
        self.latitude: float | None = None
        self.longitude: float | None = None
        self.gps_accuracy: float | None = None
        self.battery: int | None = None
        self.altitude: float | None = None
        self.speed_mps: float | None = None
        self.bearing: float | None = None
        self.provider: str | None = None
        self.screen_off: bool | None = None
        self.power_save_mode: bool | None = None
        self.immediate_report: bool | None = None
        self.last_update: datetime | None = None

    @property
    def update_interval_seconds(self) -> int:
        return clamp_interval(
            self.entry.options.get(
                CONF_UPDATE_INTERVAL,
                self.entry.data.get(CONF_UPDATE_INTERVAL, DEFAULT_UPDATE_INTERVAL),
            )
        )

    @property
    def speed_kmh(self) -> float | None:
        if self.speed_mps is None:
            return None
        return self.speed_mps * 3.6

    async def async_load(self) -> None:
        data = await self._store.async_load()
        if data:
            self._apply_dict(data, set_time_if_missing=False)

    async def async_update_from_payload(self, payload: dict[str, Any]) -> bool:
        if not self._apply_dict(payload, set_time_if_missing=True):
            _LOGGER.warning("Invalid location payload: %s", payload)
            return False
        await self._store.async_save(self.to_dict())
        async_dispatcher_send(self.hass, f"{SIGNAL_LOCATION_UPDATE}_{self.entry_id}")
        return True

    def _apply_dict(
        self, data: dict[str, Any], *, set_time_if_missing: bool
    ) -> bool:
        lat = _as_float(data.get(ATTR_LATITUDE))
        lon = _as_float(data.get(ATTR_LONGITUDE))
        if lat is None or lon is None:
            return False
        if not (-90 <= lat <= 90) or not (-180 <= lon <= 180):
            return False
        self.latitude = lat
        self.longitude = lon
        acc = _as_float(data.get(ATTR_GPS_ACCURACY))
        if acc is not None:
            self.gps_accuracy = acc
        bat = _as_int(data.get(ATTR_BATTERY))
        if bat is not None:
            self.battery = max(0, min(100, bat))
        alt = _as_float(data.get(ATTR_ALTITUDE))
        if alt is not None:
            self.altitude = alt
        speed = _as_float(data.get(ATTR_SPEED))
        if speed is not None:
            self.speed_mps = speed
        bearing = _as_float(data.get(ATTR_BEARING))
        if bearing is not None:
            self.bearing = bearing
        provider = data.get(ATTR_PROVIDER)
        if provider is not None:
            self.provider = str(provider)
        screen_off = _as_bool(data.get(ATTR_SCREEN_OFF))
        if screen_off is not None:
            self.screen_off = screen_off
        power_save = _as_bool(data.get(ATTR_POWER_SAVE_MODE))
        if power_save is not None:
            self.power_save_mode = power_save
        immediate = _as_bool(data.get(ATTR_IMMEDIATE_REPORT))
        if immediate is not None:
            self.immediate_report = immediate

        ts = data.get(ATTR_TIMESTAMP)
        if ts is not None:
            try:
                if isinstance(ts, (int, float)):
                    val = float(ts)
                    if val > 1e12:
                        val = val / 1000.0
                    self.last_update = datetime.fromtimestamp(val, tz=timezone.utc)
                elif isinstance(ts, str):
                    parsed = dt_util.parse_datetime(ts)
                    if parsed is not None:
                        self.last_update = parsed
            except (TypeError, ValueError, OSError):
                self.last_update = dt_util.utcnow()
        elif set_time_if_missing:
            self.last_update = dt_util.utcnow()
        return True

    def to_dict(self) -> dict[str, Any]:
        data: dict[str, Any] = {}
        if self.latitude is not None:
            data[ATTR_LATITUDE] = self.latitude
        if self.longitude is not None:
            data[ATTR_LONGITUDE] = self.longitude
        if self.gps_accuracy is not None:
            data[ATTR_GPS_ACCURACY] = self.gps_accuracy
        if self.battery is not None:
            data[ATTR_BATTERY] = self.battery
        if self.altitude is not None:
            data[ATTR_ALTITUDE] = self.altitude
        if self.speed_mps is not None:
            data[ATTR_SPEED] = self.speed_mps
        if self.bearing is not None:
            data[ATTR_BEARING] = self.bearing
        if self.provider is not None:
            data[ATTR_PROVIDER] = self.provider
        if self.screen_off is not None:
            data[ATTR_SCREEN_OFF] = self.screen_off
        if self.power_save_mode is not None:
            data[ATTR_POWER_SAVE_MODE] = self.power_save_mode
        if self.immediate_report is not None:
            data[ATTR_IMMEDIATE_REPORT] = self.immediate_report
        if self.last_update is not None:
            data[ATTR_TIMESTAMP] = self.last_update.timestamp()
        return data

    @property
    def extra_attributes(self) -> dict[str, Any]:
        attrs: dict[str, Any] = {}
        if self.altitude is not None:
            attrs[ATTR_ALTITUDE] = round(self.altitude, 2)
        if self.speed_kmh is not None:
            attrs["speed_kmh"] = round(self.speed_kmh, 2)
        if self.bearing is not None:
            attrs[ATTR_BEARING] = round(self.bearing, 2)
        if self.provider is not None:
            attrs[ATTR_PROVIDER] = self.provider
        if self.screen_off is not None:
            attrs[ATTR_SCREEN_OFF] = self.screen_off
        if self.power_save_mode is not None:
            attrs[ATTR_POWER_SAVE_MODE] = self.power_save_mode
        if self.last_update is not None:
            attrs[ATTR_TIMESTAMP] = self.last_update.isoformat()
        return attrs
