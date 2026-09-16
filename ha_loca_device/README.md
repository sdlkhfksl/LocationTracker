# ha_loca_device

App **主动推送**定位到 Home Assistant 公网 Webhook（外网可用）。

## 流程

1. App 填写 HA 根地址，自动生成 ID，得到 `https://你的HA/api/webhook/<id>`
2. HA 添加本集成：设备名 + 同一 Webhook ID + 上报间隔
3. App 按间隔 POST JSON；成功响应里带 `update_interval`，App 自动同步 HA 配置的间隔

## 安装

复制到 `config/custom_components/ha_loca_device` 后重启 HA。

## 调试

```bash
curl -X POST "https://你的HA/api/webhook/<id>" \
  -H "Content-Type: application/json" \
  -d '{"latitude":31.23,"longitude":121.47,"battery":80,"gps_accuracy":10}'
```
