
## 作业一
&ensp;&ensp;&ensp;&ensp;作业要求：使用GCLogAnalysis.java 自己演练一遍串行/并行/CMS/G1的案例。 

&ensp;&ensp;&ensp;&ensp;用于测试的代码如下：

```java
package gc;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

public class GCLogAnalysis {
    private static Random random = new Random();

    public static void main(String[] args) {
        // 当前毫秒时间戳
        long startMillis = System.currentTimeMillis();
        // 持续运行毫秒数; 可根据需要进行修改
        long timeoutMillis = TimeUnit.SECONDS.toMillis(5);
        // 结束时间戳
        long endMillis = startMillis + timeoutMillis;
        LongAdder counter = new LongAdder();
        System.out.println("正在执行...");
        // 缓存一部分对象; 进入老年代
        int cacheSize = 2000;
        Object[] cachedGarbage = new Object[cacheSize];
        // 在此时间范围内,持续循环
        while (System.currentTimeMillis() < endMillis) {
            // 生成垃圾对象
            Object garbage = generateGarbage(100 * 1024);
            counter.increment();
            int randomIndex = random.nextInt(2 * cacheSize);
            if (randomIndex < cacheSize) {
                cachedGarbage[randomIndex] = garbage;
            }
        }
        System.out.println("执行结束!共生成对象次数:" + counter.longValue());
    }

    // 生成对象
    private static Object generateGarbage(int max) {
        int randomSize = random.nextInt(max);
        int type = randomSize % 4;
        Object result = null;
        switch (type) {
            case 0:
                result = new int[randomSize];
                break;
            case 1:
                result = new byte[randomSize];
                break;
            case 2:
                result = new double[randomSize];
                break;
            default:
                StringBuilder builder = new StringBuilder();
                String randomString = "randomString-Anything";
                while (builder.length() < randomSize) {
                    builder.append(randomString);
                    builder.append(max);
                    builder.append(randomSize);
                }
                result = builder.toString();
                break;
        }
        return result;
    }
}
```

&ensp;&ensp;&ensp;&ensp;我的电脑win10、8核、16G内存, 设置不同的堆内存大小,分别对串行、并行、CMS、G1 GC 进行测试,JVM 参数如下：
```sh
-XX:+UseSerialGC -Xmx512m -Xms512m -XX:+PrintGC -XX:+PrintGCDateStamps
-XX:+UseParallelGC -Xmx512m -Xms512m -XX:+PrintGC -XX:+PrintGCDateStamps
-XX:+UseConcMarkSweepGC -Xmx512m -Xms512m -XX:+PrintGC -XX:+PrintGCDateStamps
-XX:+UseG1GC -Xmx512m -Xms512m -XX:+PrintGC -XX:+PrintGCDateStamps
```
&ensp;&ensp;&ensp;&ensp;结果如下图所示(为了防止数据抖动,设置的持续运行时间为5秒):

| GC/MEM             | 128M | 512M    | 1G      | 2G      | 4G      | 8G      |
| ------------------ | ---- | ------- | ------- | ------- | ------- | ------- |
| UseSerialGC        | OOM  | 30028 | 63548 | 63750 | 62246 | 65193 |
| UseParallelGC      | OOM  | 21448 | 62141 | 68952 | 70403 | 75674 |
| UseConcMarkSweepGC | OOM  | 30750 | 60419 | 63951 | 56211 | 45316 |
| UseG1GC            | OOM  | 20782 | 55097 | 62881 | 69089 | 55596 |

&ensp;&ensp;&ensp;&ensp;可以看出随着内存增大，使用串行GC的JVM逐渐达到一个稳定值，这也在一定程度上说明了串行GC在垃圾收集时只能使用单个核心，处理能力有限。但是综合其他GC的数值来看，我们可以发现串行GC的值也不低，这也说明这种GC对CPU的利用率比较高，简单粗暴，比较适合小堆的JVM

&ensp;&ensp;&ensp;&ensp;再来看看并行GC，使用ParallelGC的JVM随着堆内存增大，数值也在增大，我猜是因为并行GC充分利用了多核CPU在并行清理垃圾。为了验证这个猜想，在随后的测试过程中，我将GC线程数设置为了1，此时的数据与使用串行GC的时候相差无几。这也证实了并行GC是多个CPU核心并行清理垃圾。随后我又将堆内存增加到了10g，发现数据并没有得到提升，反而下降了一些，个人猜测是因为随着堆内存的增大，需要标记和整理的对象也就越多，而GC的处理能力有上限，超出的GC的能力范围，所以数值就表现出了下降的趋势

&ensp;&ensp;&ensp;&ensp;然后看看CMS，整体数值都小于其他的GC，虽然它的STW时间比较短，只有不到5毫秒(通过以下GC日志可以看出)，但是只有 3/4 CPU核心数的线程在跑业务，所以整体数值偏低。而且在JDK9以上的版本标记为废弃，所以不推荐使用:
```sh
正在执行...
...
2020-10-28T18:26:47.500+0800: [GC (Allocation Failure)  1991439K->1578679K(4126208K), 0.0905165 secs]
2020-10-28T18:26:47.704+0800: [GC (Allocation Failure)  2124023K->1708207K(4126208K), 0.0891367 secs]
2020-10-28T18:26:47.905+0800: [GC (Allocation Failure)  2253551K->1843744K(4126208K), 0.0923112 secs]
2020-10-28T18:26:47.998+0800: [GC (CMS Initial Mark)  1843997K(4126208K), 0.0002948 secs]
2020-10-28T18:26:48.116+0800: [GC (Allocation Failure)  2389088K->1972025K(4126208K), 0.0838131 secs]
2020-10-28T18:26:48.201+0800: [GC (CMS Final Remark)  1972105K(4126208K), 0.0043889 secs]
2020-10-28T18:26:48.319+0800: [GC (Allocation Failure)  1062257K->656105K(4126208K), 0.0342827 secs]
2020-10-28T18:26:48.449+0800: [GC (Allocation Failure)  1201449K->802682K(4126208K), 0.0362377 secs]
...
执行结束!共生成对象次数:56304.0
```
&ensp;&ensp;&ensp;&ensp;最后在来看一下G1 GC,在堆内存低于8个G的时候数值逐渐增大，这点还可以理解。但是当设置堆内存为8G的时候数值竟然还不如串行GC（重复了N多次，仍达不到预期值）这个就有点不太理解为什么？？
