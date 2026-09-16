"""Device tracker for ha_loca_device."""
from __future__ import annotations

from typing import Any

from homeassistant.components.device_tracker import SourceType, TrackerEntity
from homeassistant.config_entries import ConfigEntry
from homeassistant.core import HomeAssistant, callback
from homeassistant.helpers.device_registry import DeviceInfo
from homeassistant.helpers.dispatcher import async_dispatcher_connect
from homeassistant.helpers.entity_platform import AddEntitiesCallback
from homeassistant.util import slugify

from .const import DOMAIN, MANUFACTURER, SIGNAL_LOCATION_UPDATE
from .coordinator import LocaDeviceCoordinator


async def async_setup_entry(
    hass: HomeAssistant,
    entry: ConfigEntry,
    async_add_entities: AddEntitiesCallback,
) -> None:
    coordinator: LocaDeviceCoordinator = hass.data[DOMAIN][entry.entry_id]
    async_add_entities([LocaDeviceTracker(coordinator, entry)])


class LocaDeviceTracker(TrackerEntity):
    _attr_should_poll = False
    _attr_has_entity_name = True
    _attr_name = None
    _attr_translation_key = "location"

    def __init__(
        self, coordinator: LocaDeviceCoordinator, entry: ConfigEntry
    ) -> None:
        self._coordinator = coordinator
        self._entry = entry
        slug = slugify(coordinator.device_name)
        self._attr_unique_id = f"{entry.entry_id}_tracker"
        self._attr_suggested_object_id = f"{slug}_loca"
        self._attr_device_info = DeviceInfo(
            identifiers={(DOMAIN, entry.entry_id)},
            manufacturer=MANUFACTURER,
            name=coordinator.device_name,
            model="LocationTracker",
        )

    @property
    def latitude(self) -> float | None:
        return self._coordinator.latitude

    @property
    def longitude(self) -> float | None:
        return self._coordinator.longitude

    @property
    def location_accuracy(self) -> int:
        if self._coordinator.gps_accuracy is None:
            return 0
        return int(self._coordinator.gps_accuracy)

    @property
    def battery_level(self) -> int | None:
        return self._coordinator.battery

    @property
    def source_type(self) -> SourceType:
        return SourceType.GPS

    @property
    def icon(self) -> str:
        return "mdi:cellphone-marker"

    @property
    def extra_state_attributes(self) -> dict[str, Any]:
        return self._coordinator.extra_attributes

    @callback
    def _handle_update(self) -> None:
        self.async_write_ha_state()

    async def async_added_to_hass(self) -> None:
        self.async_on_remove(
            async_dispatcher_connect(
                self.hass,
                f"{SIGNAL_LOCATION_UPDATE}_{self._entry.entry_id}",
                self._handle_update,
            )
        )
