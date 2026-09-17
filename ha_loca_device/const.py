"""Constants for ha_loca_device."""

from homeassistant.const import Platform

DOMAIN = "ha_loca_device"
MANUFACTURER = "LocationTracker"

CONF_WEBHOOK_ID = "webhook_id"
CONF_UPDATE_INTERVAL = "update_interval"

DEFAULT_UPDATE_INTERVAL = 3600
MIN_UPDATE_INTERVAL = 10
MAX_UPDATE_INTERVAL = 10800

STORAGE_KEY = "ha_loca_device"
STORAGE_VERSION = 1

SIGNAL_LOCATION_UPDATE = f"{DOMAIN}_location_update"

ATTR_ALTITUDE = "altitude"
ATTR_BATTERY = "battery"
ATTR_BEARING = "bearing"
ATTR_GPS_ACCURACY = "gps_accuracy"
ATTR_IMMEDIATE_REPORT = "immediate_report"
ATTR_LATITUDE = "latitude"
ATTR_LONGITUDE = "longitude"
ATTR_POWER_SAVE_MODE = "power_save_mode"
ATTR_PROVIDER = "provider"
ATTR_SCREEN_OFF = "screen_off"
ATTR_SPEED = "speed"
ATTR_TIMESTAMP = "timestamp"

PLATFORMS = [Platform.DEVICE_TRACKER, Platform.SENSOR]
