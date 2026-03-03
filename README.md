# EImage

### 更新内容：
> 需要使用Shizuku授权使用

- 调整了部分UI（排序弹窗显示不全）
- 增加了Webp分类
- 支持：先将Webp转为GIF后(不支持alpha通道)，再进行分享
- 更改包名为hua.dy.image2(原：hua.dy.image)
- 支持将heic动图转为GIF(使用ffmpeg，体积增大)
- 名字更改为EImage，现在可以自定义扫描路径方案
- 使用AI优化了UI和逻辑
- 支持多选分享、多选webp转gif、多选heic动图转gif
- 新增vvic分类(火山引擎的veImageX)，无法预览图片，可分享至抖音查看图片

---



用于扫描抖音的沙盒目录，读取表情包信息，并用在其他app

使用Android的SAF框架，进行一些文件权限申请，扫描一些图片文件到自己的沙盒环境，并不会对源文件进行操作。  
将所有信息存储在一张表内，方便之后查询。  
使用了Compose 技术完成，以及Room和Paging技术做到展示所有图片。 

1/25号新增了根据file type检索图片

忘记说了，单击方向键是翻页，双击是回到顶部和去底部

4/18 添加了以Shizuku方式的检索抖音图片

Use Android's SAF framework to apply for some file permissions, scan some image files to your own sandbox environment, and will not operate on the source files.  
Store all information in a table for easy query later.  
Compose technology is used to complete, as well as Room and Paging technology to display all pictures.  


---

### Acknowledge 

- [webp2gif](https://github.com/init-01/webp2gif)
- [gif.h](https://github.com/charlietangora/gif-h)
- [libwebp](https://developers.google.com/speed/webp)
- AndroidX,Kotlin,Shizuku,Etc..
