AndroidFileExplorer
===================

This is a tool for android developers to help them manage android file system more convenient.<br>
Have you been tired typing `adb shell ls -al` or `adb pull something` or `adb remount; adb push something /system/app`?Yes, I do.I think typing commands is much slower than graphical operations. This File Explorer is modified based on DDMS's File Explorer. It's much easy to use than the original one.

Overview
--------
![](https://raw.githubusercontent.com/lrscp/AndroidFileExplorer/master/pics/p1.jpg)

How To Use It
---------
- **Push**

	simply drag the file from your pc to the window

- **Pull**

	right click on the file or directory and choose `pull and open explorer`.

	**notice**: when the file is pulled out, you should move the file from the explorer before next pull. Next pull operation will delete the previous one.

- **Copy**
	
	the copy operation is only support copy from one location on the device to the other location on the device. Right click on the file or directory and choose `copy`.

- **Cut**
	
	similar to copy

- **Paste**
	
	similar to copy

- **Delete**
	
	similar to copy

- **Delete**
	
	similar to copy

- **Remount**
	
	Open `command` menu, choose `remount`

- **Reboot**
	
	Open `command` menu, choose `reboot`

Contact me
----------
* lrscp(675486378@qq.com)