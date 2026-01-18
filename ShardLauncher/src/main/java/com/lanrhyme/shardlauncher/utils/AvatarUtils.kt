package com.lanrhyme.shardlauncher.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Paint
import androidx.core.graphics.createBitmap
import com.lanrhyme.shardlauncher.game.account.Account
import java.io.File
import java.io.IOException
import kotlin.math.roundToInt

object AvatarUtils {

    fun getAvatarFromAccount(context: Context, account: Account, size: Int = 64): Bitmap {
        val skinFile = File(context.filesDir, "skins/${account.profileId}.png")
        if (skinFile.exists()) {
             try {
                val bitmap = BitmapFactory.decodeFile(skinFile.absolutePath)
                if (bitmap != null) {
                    return getAvatar(bitmap, size)
                }
             } catch (e: Exception) {
                 Logger.lError("Failed to load avatar locally", e)
             }
        }
        return getDefaultAvatar(context, size)
    }

    private fun getDefaultAvatar(context: Context, size: Int): Bitmap {
        // Assuming we have stev_skin.png or similar in assets or drawables. 
        // For now, let's try to generate a simple colored bitmap or load from resource if possible.
        // Since I don't have easy access to assets("steve.png") right now without verifying it exists, 
        // I'll return a placeholder or try to load a drawable.
        // But for safety, I will rely on the UI to handle null or returning a generic bitmap.
        // Actually, let's create a generic Steve face programmatically if needed, or better, 
        // return a recognizable default.
        // Waiting: I'll assume we can use the app's resource R.drawable.img_steve if available and decode it.
        // But context.resources.getDrawable is deprecated/complex for Bitmap.
        // Let's just return a gray bitmap for now if 'steve.png' asset isn't guaranteed.
        // The user has 'R.drawable.img_steve'.
        
        return try {
             val drawable = com.lanrhyme.shardlauncher.R.drawable.img_steve
             val bitmap = BitmapFactory.decodeResource(context.resources, drawable)
             getAvatar(bitmap, size)
        } catch (e: Exception) {
            createBitmap(size, size).apply { eraseColor(android.graphics.Color.GRAY) }
        }
    }

    private fun getAvatar(skin: Bitmap, size: Int): Bitmap {
        val faceOffset = (size / 18.0).roundToInt().toFloat()
        val scaleFactor = skin.width / 64.0f
        val faceSize = (8 * scaleFactor).roundToInt()
        
        // Face
        val faceBitmap = Bitmap.createBitmap(skin, faceSize, faceSize, faceSize, faceSize, null as Matrix?, false)
        // Hat (Overlay)
        val hatBitmap = Bitmap.createBitmap(skin, (40 * scaleFactor).roundToInt(), faceSize, faceSize, faceSize, null as Matrix?, false)
        
        val avatar = createBitmap(size, size)
        val canvas = android.graphics.Canvas(avatar)
        
        val faceScale = ((size - 2 * faceOffset) / faceSize)
        val hatScale = (size.toFloat() / faceSize)
        
        var matrix = Matrix()
        matrix.postScale(faceScale, faceScale)
        val newFaceBitmap = Bitmap.createBitmap(faceBitmap, 0, 0, faceSize, faceSize, matrix, false)
        
        matrix = Matrix()
        matrix.postScale(hatScale, hatScale)
        val newHatBitmap = Bitmap.createBitmap(hatBitmap, 0, 0, faceSize, faceSize, matrix, false)
        
        canvas.drawBitmap(newFaceBitmap, faceOffset, faceOffset, Paint(Paint.ANTI_ALIAS_FLAG))
        canvas.drawBitmap(newHatBitmap, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG))
        
        return avatar
    }
}
