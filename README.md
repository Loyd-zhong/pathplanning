1.读取了每条边的状态速度。
2.每条边的长度用distance属性确定，而非欧几里得距离。
3.优化了冲突检测，添加了handleMaxretries来解决超过最大重试次数的问题。
4.添加了定点找车方法，在getlateAgv中。
5.优化了地图加载方法，现在rever不管是什么属性都会储存单向边。
6.冲突检测添加了对向边检测，进而避免对向行驶。
7.添加了每条边上关于状态速度和默认速度的比较，路径规划的输出Path类添加了每条边上行驶的速度。
8.添加了定车找点的方法，也在getlateAGV中。9.冲突检测方法由handleMaxretries转为resolvePathConflicts
