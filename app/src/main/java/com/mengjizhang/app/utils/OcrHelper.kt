package com.mengjizhang.app.utils

import android.graphics.Bitmap
import com.mengjizhang.app.api.SiliconFlowApi

/**
 * OCR 文字识别助手类
 * 使用硅基流动的视觉语言模型进行智能账单识别
 */
object OcrHelper {

    /**
     * 识别图片中的文字（纯 OCR）
     */
    suspend fun recognizeText(bitmap: Bitmap): String {
        val result = SiliconFlowApi.recognizeText(bitmap)
        return result.getOrElse { throw it }
    }

    /**
     * 识别图片中的文字（返回 Result）
     */
    suspend fun recognizeTextResult(bitmap: Bitmap): Result<String> {
        return SiliconFlowApi.recognizeText(bitmap)
    }

    /**
     * 智能识别账单/收据
     * 使用视觉语言模型理解图片内容，提取金额、商家等信息
     */
    suspend fun recognizeReceipt(bitmap: Bitmap): Result<ReceiptInfo> {
        return SiliconFlowApi.recognizeReceipt(bitmap).map { result ->
            ReceiptInfo(
                totalAmount = result.amount,
                merchant = result.merchant,
                category = result.category,
                note = result.note,
                rawText = result.rawText
            )
        }
    }

    /**
     * 账单识别结果
     */
    data class ReceiptInfo(
        val totalAmount: Double?,      // 总金额
        val merchant: String?,         // 商家名称
        val category: String?,         // 推断的分类
        val note: String?,             // 备注
        val rawText: String            // 原始识别文本
    )

    /**
     * 商品信息（保留兼容性）
     */
    data class ItemInfo(
        val name: String,
        val amount: Double?
    )

    /**
     * 解析收据/小票文字（保留兼容性，用于纯文字解析场景）
     */
    fun parseReceipt(text: String): ReceiptInfo {
        val lines = text.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        var totalAmount: Double? = null

        // 常见的合计关键词
        val totalKeywords = listOf("合计", "总计", "总额", "应付", "实付", "Total", "TOTAL", "金额")

        // 金额匹配模式 - 改进：只匹配合理范围的金额（0.01-99999.99）
        val amountPattern = Regex("""(?<![0-9])(\d{1,5}(?:\.\d{1,2})?)(?![0-9])""")

        for (line in lines) {
            // 检查是否包含合计金额
            val containsTotalKeyword = totalKeywords.any { line.contains(it, ignoreCase = true) }

            if (containsTotalKeyword) {
                val matches = amountPattern.findAll(line)
                val amounts = matches.mapNotNull {
                    it.groupValues[1].toDoubleOrNull()
                }.filter { it in 0.01..99999.99 }.toList()

                // 取最大的金额作为合计
                if (amounts.isNotEmpty()) {
                    totalAmount = amounts.maxOrNull()
                }
            }
        }

        // 如果没有找到合计，尝试从带货币符号的行提取
        if (totalAmount == null) {
            val currencyPattern = Regex("""[¥￥-]\s*(\d{1,5}(?:\.\d{1,2})?)""")
            for (line in lines) {
                val match = currencyPattern.find(line)
                if (match != null) {
                    val amount = match.groupValues[1].toDoubleOrNull()
                    if (amount != null && amount in 0.01..99999.99) {
                        if (totalAmount == null || amount > totalAmount!!) {
                            totalAmount = amount
                        }
                    }
                }
            }
        }

        return ReceiptInfo(
            totalAmount = totalAmount,
            merchant = null,
            category = inferCategory(text),
            note = null,
            rawText = text
        )
    }

    /**
     * 根据识别内容推断分类
     */
    fun inferCategory(text: String): String {
        val lowerText = text.lowercase()

        return when {
            // 餐饮
            listOf("餐厅", "饭店", "食堂", "外卖", "美团", "饿了么", "肯德基", "麦当劳",
                "奶茶", "咖啡", "星巴克", "瑞幸", "早餐", "午餐", "晚餐", "饮料").any {
                lowerText.contains(it)
            } -> "餐饮"

            // 交通
            listOf("出租车", "滴滴", "公交", "地铁", "高铁", "火车", "机票", "加油站",
                "停车", "uber", "出行").any { lowerText.contains(it) } -> "交通"

            // 购物
            listOf("超市", "商场", "淘宝", "京东", "天猫", "拼多多", "购物", "服装",
                "衣服", "鞋").any { lowerText.contains(it) } -> "购物"

            // 娱乐
            listOf("电影", "ktv", "游戏", "演唱会", "门票", "景区", "旅游", "酒店").any {
                lowerText.contains(it)
            } -> "娱乐"

            // 居家
            listOf("水费", "电费", "煤气", "物业", "房租", "维修", "家具", "家电").any {
                lowerText.contains(it)
            } -> "居家"

            // 医疗
            listOf("医院", "药店", "药房", "诊所", "体检", "挂号").any {
                lowerText.contains(it)
            } -> "医疗"

            // 教育
            listOf("学校", "培训", "课程", "书店", "学费", "教育").any {
                lowerText.contains(it)
            } -> "教育"

            else -> "其他"
        }
    }
}
