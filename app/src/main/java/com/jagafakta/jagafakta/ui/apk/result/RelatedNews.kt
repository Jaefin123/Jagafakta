package com.jagafakta.jagafakta.ui.apk.result

import android.os.Parcel
import android.os.Parcelable

data class RelatedNews(
    val title: String,
    val sourceName: String,
    val urlToImage: String,
    val url: String
) : Parcelable {

    private constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: ""
    )

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(title)
        dest.writeString(sourceName)
        dest.writeString(urlToImage)
        dest.writeString(url)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<RelatedNews> {
        override fun createFromParcel(parcel: Parcel): RelatedNews = RelatedNews(parcel)
        override fun newArray(size: Int): Array<RelatedNews?> = arrayOfNulls(size)
    }
}
