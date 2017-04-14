### AndroidVideoHelper

> Created by Mingkuan At 2017-04-14

#### 实现功能

- 通过ADB forward进行端口转发，把本机端口转发到Android手机内
- 建立Socket，PC端发送指令到手机开始录制视频
- 手机开始录制视频后，返回视频文件保存的地址
- PC端发送停止录制的指令到手机后，使用adb pull把视频文件拉取会本地

