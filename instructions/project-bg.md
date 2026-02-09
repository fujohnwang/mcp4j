## 项目刚性要求

1. 最少依赖： 暂时只基于com.sun.net.httpserver实现http通信，使用jackson作json/json-schema相关处理。
2. 只支持最新的streamable http通信的mcp server实现，不支持stdio方式以及不再推荐的sse方式。 （stdio实际上有太多方式和选择了，所以没必要再在这个项目里支持）


