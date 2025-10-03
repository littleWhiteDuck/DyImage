package hua.dy.image.bean

import android.os.Parcel
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import hua.dy.image.utils.FileType


val ImageBean.isGif get() = fileType == FileType.GIF
val ImageBean.isWebp get() = fileType == FileType.WEBP
val ImageBean.isHeic get() = fileType == FileType.HEIC


@Entity("dy_image")
data class ImageBean(
    @PrimaryKey
    val md5: String = "-1",
    @ColumnInfo(name = "image_path")
    val imagePath: String = "",
    @ColumnInfo(name = "file_length", defaultValue = "0")
    val fileLength: Long = 0,
    @ColumnInfo(name = "file_time")
    val fileTime: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "file_type")
    val fileType: FileType = FileType.PNG,
    @ColumnInfo(name = "file_name", defaultValue = "")
    val fileName: String = "",
    @ColumnInfo(name = "second_menu", defaultValue = "")
    val secondMenu: String = "",
    @ColumnInfo(name = "scan_time", defaultValue = "0")
    val scanTime: Long = 0,
    @ColumnInfo(name = "cache_path", defaultValue = "")
    val cachePath: String = ""
): Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?:"",
        parcel.readString() ?:"",
        parcel.readLong(),
        parcel.readLong(),
        parcel.readSerializable() as FileType,
        parcel.readString() ?: "",
        parcel.readString() ?:"",
        parcel.readLong(),
        parcel.readString()?:""
    )

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(md5)
        dest.writeString(imagePath)
        dest.writeLong(fileLength)
        dest.writeLong(fileTime)
        dest.writeSerializable(fileType)
        dest.writeString(fileName)
        dest.writeString(secondMenu)
        dest.writeLong(scanTime)
        dest.writeString(cachePath)
    }

    companion object CREATOR : Parcelable.Creator<ImageBean> {
        override fun createFromParcel(parcel: Parcel): ImageBean {
            return ImageBean(parcel)
        }

        override fun newArray(size: Int): Array<ImageBean?> {
            return arrayOfNulls(size)
        }
    }


}