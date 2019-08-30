# Distributed-Configuration-Center
基于Zookeeper的分布式配置中心  
# 介绍  
分布式配置中心：使用Zookeeper达到了数据强一致性,利用Watch机制,动态更改节点值即刻生效,无需重启系统
# 使用说明
@ConfigNode  
@ConfigNode(nodename = "jdbc")  
该注解加在类上  
抽象为zk主节点下的配置节点,若在注解中没有配置名称则在zk中这个配置节点的名称应为被修饰的类名  
@ConfigValue  
@ConfigValue(value = "account")
该注解加在属性上  
抽象为zk主节点下的配置节点的配置节点属性,若在注解中没有配置名称则在zk中这个配置节点的名称应为被修饰的属性名   
使用代码  
1 本地使用
new ConfigCenterManager("/myconfig","util");  
第一个参数 主节点名称(必须有/) 第二个参数 扫描该包下的配置了@ConfigNode类    
2 传入Zookeeper  
new ConfigCenterManager(new ZooKeeper("ip:port",10000,null),"/myconfig","util");  
第一个参数 传入自定义Zookeeper对象 第二个参数 主节点名称(必须有/) 第三个参数 扫描该包下的配置了@ConfigNode类    


# 示例 JDBC配置类  
进行zkCli.sh 执行命令  
create /myconfig mainnode  
create /myconfig/JdbcUtil class  
create /myconfig/JdbcUtil/username root  
create /myconfig/JdbcUtil/password 123456  
create /myconfig/JdbcUtil/url jdbc:mysql://localhost:3306/light?useUnicode=true&characterEncoding=UTF-8&useSSL=false
create /myconfig/JdbcUtil/drive com.mysql.jdbc.Driver  

![Image text](https://github.com/2531251963/Distributed-Configuration-Center/blob/master/img/1.png)  
动态更改节点的值  
set /myconfig/JdbcUtil/password 12345  
此时密码错误!

# 2  
进行zkCli.sh 执行命令  
create /myconfig mainnode  
create /myconfig/jdbc class  
create /myconfig/jdbc/account root  
create /myconfig/jdbc/password 123456  
create /myconfig/JdbcUtil/url jdbc:mysql://localhost:3306/light?useUnicode=true&characterEncoding=UTF-8&useSSL=false
create /myconfig/JdbcUtil/drive com.mysql.jdbc.Driver  

![Image text](https://github.com/2531251963/Distributed-Configuration-Center/blob/master/img/2.png)   
动态更改节点的值  
set /myconfig/jdbc/password 12345  
此时密码错误!


