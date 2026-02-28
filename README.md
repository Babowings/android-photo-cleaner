# PureGallery

一款离线优先的 Android 相册清理工具，使用手势快速筛选照片。

## 功能

- 左滑：下一张（保留）
- 右滑：上一张（回看）
- 上滑：标记待删除
- 下滑：标记收藏
- 清理列表：进入复核页后统一确认删除
- 全程本地处理，不依赖网络

## 技术栈

- Kotlin
- Jetpack Compose
- Room
- Coil
- MediaStore API

## 环境要求

- Android Studio（建议最新稳定版）
- JDK 17
- Android SDK（`minSdk 29`，`targetSdk 35`）

## 本地运行

1. 用 Android Studio 打开项目根目录。
2. 等待 Gradle 同步完成。
3. 连接真机或启动模拟器。
4. 运行 `app` 模块。

## 权限说明

- Android 13+：`READ_MEDIA_IMAGES`
- Android 12 及以下：`READ_EXTERNAL_STORAGE`
- 删除/收藏操作走系统授权流程（`MediaStore` 请求）

## 项目结构

- `app/src/main/java/com/puregallery/ui/gallery`：主浏览与手势逻辑
- `app/src/main/java/com/puregallery/ui/review`：删除复核页
- `app/src/main/java/com/puregallery/media`：MediaStore 扫描、删除、收藏执行
- `app/src/main/java/com/puregallery/data`：Room 数据层

