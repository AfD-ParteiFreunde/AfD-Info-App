package de.afd.parteiapp

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Sends a browser-like User-Agent (plus a matching Referer) for network
 * image loads. Several party sites reject non-browser or hotlinked image
 * requests, which made some member portraits intermittently disappear.
 */
class AfDApplication : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader {
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(Interceptor { chain ->
                val origin = chain.request().url.let { "${it.scheme}://${it.host}/" }
                val request = chain.request().newBuilder()
                    .header(
                        "User-Agent",
                        "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 " +
                            "(KHTML, like Gecko) Chrome/126.0 Mobile Safari/537.36",
                    )
                    .header("Accept", "image/avif,image/webp,image/*,*/*;q=0.8")
                    .header("Referer", origin)
                    .build()
                chain.proceed(request)
            })
            .build()

        return ImageLoader.Builder(this)
            .okHttpClient(client)
            .crossfade(true)
            .build()
    }
}
