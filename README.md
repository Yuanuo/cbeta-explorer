# 智悲乐藏（CBETA Explorer）

这是一款基于CBETA经藏数据进行本地阅读、记录、搜索的应用程序！

## 重要！！
  本程序只是一个阅读程序，本身不提供CBETA经藏数据，由于其数据打包压缩仍然非常庞大！
  运行本程序需要的数据，是CBETA官方提供的阅读器（CBReader）所包含的数据。通常是存在“Bookcase\CBETA”的目录中。


## 名称由来
+  智、悲：佛法之本末。初时不离。道时不离。果时不离！
+  乐：
   - 读为Yue，即通“阅”。阅藏读经！
   - 读为Le，即通“乐”。乐于经藏。读于经藏。更生法喜。是得真乐！
   - 读为Yao，通繁字“樂”。经中常有愿樂欲闻。欢喜勇樂于法！
+  藏：经藏、法藏。能生二乐。故如宝藏！


## 为了满足读经时产生的几个基本需求而开发
*  速度快。启动速度快，启动后立即进入阅读模式！
*  检索快。快速检索经名、章节名、作者、译者等，即输即搜！
*  护眼。支持界面暗黑模式适于长时间阅读！
*  记录。作为阅读程序，能记住阅读历史和进度非常重要。支持重新打开程序或典籍时恢复到上一次关闭时的状态，包括已打开的书籍和正在阅读的进度！
*  简繁体。支持自由切换简繁体显示典籍内容！
*  全文检索。支持！

如果你也有这样的需求，希望本程序可以为你带来帮助。

## 功能特点
本着能看、能记、能搜的原则，本阅读器目前已完整实现这三个特性

### 基础
* 离线。本机运行无需联网，不受内外网络速度影响
* 速度快。启动快、检索快、搜索快
* 简繁体。不会输入“正确”的繁体字？输入简体字即可检索！不习惯阅读繁体字？选择以简体字查看即可！

### 能看
* 支持HiDPI。
* 支持深色模式。长时阅读或夜间阅读可以护眼
* 支持阅读视图中缩放字号大小
* 支持按原书分行查看
* 支持显示编注。支持以多种展示方式显示编注。原编注、CBETA编注、#号标记、着色（被编注的文字）

### 能记
* 记录在读典籍。程序启动时恢复上一次关闭时的在读典籍视图，立即恢复阅读状态而不需要重新查找典籍并打开
* 记录阅读进度。打开典籍时恢复上一次关闭时的进度状态
* 支持书签。用于收藏阅读进度位置等
* 支持收藏。用于收藏教证等文字内容

### 能搜
* 支持页内查找。在阅读视图中查找本页内容
* 支持快速检索。可检索典籍ID、名称、作者、译者、作译者年代等
* 支持全文检索。

  基本理念：法藏深广，用户不可能记得完整的原文，故而不需要精确输入字句匹配的原文才能搜索到结果！

  本阅读器提供基于关键词的全文检索功能，支持搜索“任意”字、词、句，所谓“任意”即是指典籍中不一定存在而你能想到/有模糊印象的关键词。
  
  在搜索结果中可以按照 典籍类目、作译者年代、经藏类目 等进行过滤结果。

  搜索结果按关键词高亮显示，点击高亮关键词将打开阅读视图并“尝试”定位到对应位置（不保证完全精确定位！）
* 以上三种检索均不区分简繁体汉字输入


## 多平台支持

由于本程序基于Java/JavaFX/OpenJFX开发，可轻易实现跨平台，目前经过测试的三个平台
*  Windows 10 （[提供MSI安装程序](https://github.com/Yuanuo/appxi-cbeta-explorer/releases)）
*  Ubuntu 20.10 （[提供DEB安装程序](https://github.com/Yuanuo/appxi-cbeta-explorer/releases)）
*  macOS Big Sur （[提供DMG安装程序](https://github.com/Yuanuo/appxi-cbeta-explorer/releases)）


## 如何使用（绿色版）

+ 本程序可工作在绿色/Portable模式（目前测试过Windows平台）

    工作在此模式时用户数据和主程序在同一目录，此时若使用“卸载”功能将删除整个目录从而导致用户数据丢失，故请勿在些模式时使用系统“卸载”功能。

+ 在此提供Portable版本并带有CBETA经藏数据的压缩包云盘下载地址
  + [云盘主目录](https://cloud.189.cn/t/nInQ7zyA7zMr#4p5z)（访问码：4p5z）
  + 阅读器 + CBETA经藏数据 （文件：cbeta-explorer-21.02.23-data.zip）
  + 阅读器 + CBETA经藏数据 + 全文索引数据库 （文件：cbeta-explorer-21.02.23-data&index.zip）


## 如何使用

+ 1、准备程序。本程序默认提供跨三种平台的安装包
  + Windows（[MSI安装程序](https://github.com/Yuanuo/appxi-cbeta-explorer/releases)）
  + MacOS（[DEB安装程序](https://github.com/Yuanuo/appxi-cbeta-explorer/releases)）
  + Ubuntu（[DMG安装程序](https://github.com/Yuanuo/appxi-cbeta-explorer/releases)）
  + [云盘下载地址](https://cloud.189.cn/t/nInQ7zyA7zMr#4p5z)（访问码：4p5z）
+ 2、准备数据。本程序仅使用CBETA官方的“經文資料檔”，如果本机已经有CBReader阅读器，此阅读器本身即包含了官方“經文資料檔”数据（数据通常在CBReader.exe所在目录），无需再次下载。
  + [下载地址](http://www.cbeta.org/download/cbreader.htm) ，在此页面中查找 “經文資料檔”并选择最新版本下载，下载完成后解压到本地磁盘即可
+ 3、启动程序。本程序安装完成后会产生桌面快捷方式，请通过快捷方式启动
+ 4、设置CBETA经藏数据目录，此操作一般只发生在首次使用时，按提示选择存在 “Bookcase”的CBETA经藏数据目录即可
+ 5、Enjoy！

  小提示：首次启动（或本地经藏数据已更新）时，程序会自动建立全文索引，此时系统CPU资源使用量增大是正常现象（一般情况下20~30分钟左右即可完成，在此期间可正常使用阅读等功能，此时开启一个搜索视图可显示实时索引进度）


## 欢迎反馈

欢迎反馈任何BUG或意见。当然也可以提新需求。
