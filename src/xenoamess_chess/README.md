# xenoamess_chess

棋类游戏模块骨架，继承 `../parent/pom.xml` 中的 Java 版本和构建配置。

Java 源码放在 `src/main/java/com/xenoamess/cyan_potion/xenoamess_chess`，测试放在 `src/test/java`。

设计文档见 [设计总览](docs/design/README.md)、[核心规则](docs/design/rules.md) 和 [基础阵营的 10 种单位](docs/design/units.md)。

在仓库根目录执行构建验证：

```shell
mvn -pl src/xenoamess_chess -am test
```
