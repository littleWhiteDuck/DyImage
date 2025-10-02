package hua.dy.image.utils

var sortValue by SharedPreferenceEntrust("sort_type", 0)

val sortList = buildList {
    add("按文件时间排序")
    add("按扫描时间排序")
    add("按文件大小排序")
}