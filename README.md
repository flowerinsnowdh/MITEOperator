# MITEOperator
以 java-agent 的方式解锁 [Minecraft Is Too Easy](https://www.mcmod.cn/class/226.html) 原版作弊和权限系统（op）

# 编译
```shell
./gradlew shadowJar
```

# 使用方法
从 releases 下载文件放到游戏（或服务器）目录 ，并添加 JVM 参数

```shell
-javaagent:miteoperator-1.0.0.jar
```

# 注意
我只是把权限解锁出来了，其他内容我都没有更改

例如 `/level` `/heal` 这些方法是原作者写的，不是我写的

# 声明
Copyright (c) 2024 flowerinsnow

只用于编译 jar 文件和使用，不允许任何人

1. 修改：任何人不得修改本项目的任何内容（包括但不限于源代码、二进制文件等）
2. 二次分发：任何不得将本项目的任何内容（包括但不限于源代码、二进制文件等）二次分发
3. 继承：任何人不得声称自己是本项目的作者或继承者
