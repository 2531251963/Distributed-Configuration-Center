package org.easyarch.configcenter;

import org.apache.zookeeper.*;
import org.easyarch.configcenter.annotation.ConfigNode;
import org.easyarch.configcenter.annotation.ConfigValue;

import java.io.File;
import java.io.FileFilter;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @ClassName ConfigCenterManager
 * @Description 基于Zookeeper分布式配置中心
 * @Author Liyihe
 * @Date 2019/08/30 上午10:55
 * @Version 1.0
 */
public class ConfigCenterManager {
    private ZooKeeper zk;
    private String mainNodeName;
    private String scanPackageName[];
    private ConfigCenterManager(){}
    public ConfigCenterManager(String mainNodeName,String ...scanPackaheName){
        this.mainNodeName=mainNodeName;
        this.scanPackageName=scanPackaheName;
        try {
            initZk();
            createMainNode();
            scanPackage();
        }catch (Exception e){
            e.printStackTrace();
            if (zk!=null){
                ReentrantLock lock = new ReentrantLock();
                lock.lock();
                if (!zk.getState().isConnected()||!zk.getState().isAlive()){
                    new ConfigCenterManager(mainNodeName,scanPackaheName);
                }
                lock.unlock();
            }
        }
    }

    public  ConfigCenterManager(ZooKeeper zk,String mainNodeName,String ...scanPackaheName) throws Exception {
        this.zk=zk;
        this.mainNodeName=mainNodeName;
        this.scanPackageName=scanPackaheName;
        if (this.zk==null){
            throw new Exception("this zookeeper client not is null ...");
        }
        if (!this.zk.getState().isConnected()){
            throw new Exception("this zookeeper client is not Connection");
        }
        createMainNode();
        scanPackage();
    }
    private  void initZk() {
        final CountDownLatch latch=new CountDownLatch(1);
        try {
            ZooKeeper zk=new ZooKeeper("localhost:2181", 1000000, new Watcher() {
                public void process(WatchedEvent watchedEvent) {
                    if(Event.KeeperState.SyncConnected == watchedEvent.getState()) {
                        latch.countDown();
                    }
                }
            });
            latch.await();
            this.zk=zk;
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    private void createMainNode() throws Exception {
        if (this.mainNodeName==null){
            throw new Exception("this mainNodeName not is null");
        }
        if (zk.exists(mainNodeName,false)==null) {
            zk.create(mainNodeName, "mainNode".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        }
    }
    private void scanPackage()throws Exception{
        if (scanPackageName==null||scanPackageName.length==0){
            throw new Exception("this packageName is not null or length = 0");
        }
        Set<Class<?>> classes = new LinkedHashSet<Class<?>>();
        for (String packagename :
                scanPackageName) {
            getClasses(packagename,classes);
        }

        getAllConfig(classes);
    }
    /**
     * 从包package中获取所有的Class
     */
    private  Set<Class<?>> getClasses(String pack,Set<Class<?>> classes_set) throws Exception {
        // 第一个class类的集合
        // 是否循环迭代
        boolean recursive = true;
        // 获取包的名字 并进行替换
        String packageDirName = pack.replace('.', '/');
        // 定义一个枚举的集合 并进行循环来处理这个目录下的things
        Enumeration<URL> dirs;

            dirs = Thread.currentThread().getContextClassLoader().getResources(packageDirName);
            // 循环迭代下去
            if (!dirs.hasMoreElements()){
                throw new Exception(pack+" not found");
            }
            while (dirs.hasMoreElements()) {
                // 获取下一个元素
                URL url = dirs.nextElement();
                // 得到协议的名称
                String protocol = url.getProtocol();
                // 如果是以文件的形式保存在服务器上
                if ("file".equals(protocol)) {
                    // 获取包的物理路径
                    String filePath = URLDecoder.decode(url.getFile(), "UTF-8");
                    // 以文件的方式扫描整个包下的文件 并添加到集合中
                    findAndAddClassesInPackageByFile(pack, filePath, recursive, classes_set);
                }
            }

        return classes_set;
    }

    /**
     * 以文件的形式来获取包下的所有Class
     */
    private void findAndAddClassesInPackageByFile(String packageName, String packagePath, final boolean recursive,
                                                        Set<Class<?>> classes) {
        // 获取此包的目录 建立一个File
        File dir = new File(packagePath);
        // 如果不存在或者 也不是目录就直接返回
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }
        // 如果存在 就获取包下的所有文件 包括目录
        File[] dirfiles = dir.listFiles(new FileFilter() {
            // 自定义过滤规则 如果可以循环(包含子目录) 或则是以.class结尾的文件(编译好的java类文件)
            public boolean accept(File file) {
                return (recursive && file.isDirectory()) || (file.getName().endsWith(".class"));
            }
        });
        // 循环所有文件
        for (File file : dirfiles) {
            // 如果是目录 则继续扫描
            if (file.isDirectory()) {
                findAndAddClassesInPackageByFile(packageName + "." + file.getName(), file.getAbsolutePath(), recursive,
                        classes);
            } else {
                // 如果是java类文件 去掉后面的.class 只留下类名
                String className = file.getName().substring(0, file.getName().length() - 6);
                try {
                    // 添加到集合中去
                    // classes.add(Class.forName(packageName + '.' +className));
                    // 如果用forName有一些不好，会触发static方法，没有使用classLoader的load干净
                    classes.add(
                            Thread.currentThread().getContextClassLoader().loadClass(packageName + '.' + className));
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    private void getAllConfig(Set<Class<?>> classes) throws Exception {
        for (Class<?> clazz :
                classes) {
            if (clazz.isAnnotationPresent(ConfigNode.class)) {
                ConfigNode configNode=clazz.getAnnotation(ConfigNode.class);
                String nodename=mainNodeName+"/";
                nodename+=configNode.nodename().trim().equals("")? clazz.getSimpleName() : configNode.nodename();
                if (zk.exists(nodename,false)==null){
                    throw new Exception(nodename+" Node not found in Zookeeper Client");
                }else {
                    Field fields[]=clazz.getDeclaredFields();
                    for (Field field:fields) {
                        if (field.isAnnotationPresent(ConfigValue.class)){
                            if (!Modifier.isStatic(field.getModifiers())){
                                throw new Exception("field : "+field.getName()+" must static");
                            }
                            String fieldname=field.getName();
                            String value=field.getAnnotation(ConfigValue.class).value();
                            String nodepath=nodename+"/";
                            nodepath+=value.trim().equals("") ? fieldname : value;
                            String res=getNodeData(nodepath,fieldname,clazz);
                            field.setAccessible(true);
                            field.set(clazz,res);
                            System.out.println("class:"+clazz.getSimpleName()+"   field:"+fieldname+"   value:"+res);
                        }
                    }
                }
            }
        }
    }
    private  String getNodeData(final String nodepath,final String fieldname,final Class<?> clazz) {
        byte data[] = new byte[0];
        try {
            if (zk.exists(nodepath,false)==null){
                System.err.println("Error: "+nodepath+" Node not found in Zookeeper Client");
                return "";
            }
            data = zk.getData(nodepath,new Watcher() {
                public void process(WatchedEvent watchedEvent) {
                    if (watchedEvent.getType().equals(Event.EventType.NodeDataChanged)) {
                        try {
                            String v=getNodeData(nodepath,fieldname,clazz);
                            System.out.println("配置更新: zk路径:"+nodepath+"   class:"+clazz.getSimpleName()+"   field:"+fieldname+"   value:"+v);
                            Field field=clazz.getDeclaredField(fieldname);
                            field.setAccessible(true);
                            field.set(clazz,v);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            },null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new String(data);
    }
}

