学习笔记

##堆外内存和非堆内存的区别在哪里
    非堆内存还是归jvm管（而且不是业务代码能涉及到的），堆外内存一般是为了绕开gc直接使用的内存，业务代码特别是JNI用的，通常情况下是可能是非Java语言或者DirectBuffer里搞的。
   
    比如说用C++写了一个lib库里面有个函数cppHello，这里面手动申请了内存，然后jvm调用，那么这个函数申请的内存，跟jvm的内存管理就没关系，但是在jvm进程里。