"""Sensors for ha_loca_device."""
from __future__ import annotations

from datetime import datetime

from homeassistant.components.sensor import (
    SensorDeviceClass,
    SensorEntity,
    SensorStateClass,
)
from homeassistant.config_entries import ConfigEntry
from homeassistant.const import (
    PERCENTAGE,
    UnitOfLength,
    UnitOfSpeed,
)
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
    slug = slugify(coordinator.device_name)
    device_info = DeviceInfo(
        identifiers={(DOMAIN, entry.entry_id)},
        manufacturer=MANUFACTURER,
        name=coordinator.device_name,
        model="LocationTracker",
    )
    async_add_entities(
        [
            LocaBatterySensor(coordinator, entry, slug, device_info),
            LocaAccuracySensor(coordinator, entry, slug, device_info),
            LocaSpeedSensor(coordinator, entry, slug, device_info),
            LocaAltitudeSensor(coordinator, entry, slug, device_info),
            LocaLastUpdateSensor(coordinator, entry, slug, device_info),
            LocaProviderSensor(coordinator, entry, slug, device_info),
        ]
    )


class LocaBaseSensor(SensorEntity):
    _attr_should_poll = False
    _attr_has_entity_name = True

    def __init__(
        self,
        coordinator: LocaDeviceCoordinator,
        entry: ConfigEntry,
        slug: str,
        device_info: DeviceInfo,
        key: str,
    ) -> None:
        self._coordinator = coordinator
        self._entry = entry
        self._attr_unique_id = f"{entry.entry_id}_{key}"
        self._attr_suggested_object_id = f"{slug}_loca_{key}"
        self._attr_device_info = device_info
        self._attr_translation_key = key

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


class LocaBatterySensor(LocaBaseSensor):
    _attr_device_class = SensorDeviceClass.BATTERY
    _attr_state_class = SensorStateClass.MEASUREMENT
    _attr_native_unit_of_measurement = PERCENTAGE
    _attr_icon = "mdi:battery"

    def __init__(self, coordinator, entry, slug, device_info) -> None:
        super().__init__(coordinator, entry, slug, device_info, "battery")

    @property
    def native_value(self) -> int | None:
        return self._coordinator.battery


class LocaAccuracySensor(LocaBaseSensor):
    _attr_device_class = SensorDeviceClass.DISTANCE
    _attr_state_class = SensorStateClass.MEASUREMENT
    _attr_native_unit_of_measurement = UnitOfLength.METERS
    _attr_icon = "mdi:crosshairs-gps"

    def __init__(self, coordinator, entry, slug, device_info) -> None:
        super().__init__(coordinator, entry, slug, device_info, "gps_accuracy")

    @property
    def native_value(self) -> float | None:
        if self._coordinator.gps_accuracy is None:
            return None
        return round(self._coordinator.gps_accuracy, 1)


class LocaSpeedSensor(LocaBaseSensor):
    _attr_device_class = SensorDeviceClass.SPEED
    _attr_state_class = SensorStateClass.MEASUREMENT
    _attr_native_unit_of_measurement = UnitOfSpeed.KILOMETERS_PER_HOUR
    _attr_icon = "mdi:speedometer"

    def __init__(self, coordinator, entry, slug, device_info) -> None:
        super().__init__(coordinator, entry, slug, device_info, "speed")

    @property
    def native_value(self) -> float | None:
        if self._coordinator.speed_kmh is None:
            return None
        return round(self._coordinator.speed_kmh, 2)


class LocaAltitudeSensor(LocaBaseSensor):
    _attr_device_class = SensorDeviceClass.DISTANCE
    _attr_state_class = SensorStateClass.MEASUREMENT
    _attr_native_unit_of_measurement = UnitOfLength.METERS
    _attr_icon = "mdi:altimeter"

    def __init__(self, coordinator, entry, slug, device_info) -> None:
        super().__init__(coordinator, entry, slug, device_info, "altitude")

    @property
    def native_value(self) -> float | None:
        if self._coordinator.altitude is None:
            return None
        return round(self._coordinator.altitude, 1)


class LocaLastUpdateSensor(LocaBaseSensor):
    _attr_device_class = SensorDeviceClass.TIMESTAMP
    _attr_icon = "mdi:clock-outline"

    def __init__(self, coordinator, entry, slug, device_info) -> None:
        super().__init__(coordinator, entry, slug, device_info, "last_update")

    @property
    def native_value(self) -> datetime | None:
        return self._coordinator.last_update


class LocaProviderSensor(LocaBaseSensor):
    _attr_icon = "mdi:satellite-variant"

    def __init__(self, coordinator, entry, slug, device_info) -> None:
        super().__init__(coordinator, entry, slug, device_info, "provider")

    @property
    def native_value(self) -> str | None:
        return self._coordinator.provider
