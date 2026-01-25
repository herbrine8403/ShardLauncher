package com.lanrhyme.shardlauncher.ui.xaml.model

import android.util.Log
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader

sealed interface XamlNode {
    val attributes: Map<String, String>
    val children: List<XamlNode>
}

data class CardNode(override val attributes: Map<String, String>, override val children: List<XamlNode>) : XamlNode
data class StackPanelNode(override val attributes: Map<String, String>, override val children: List<XamlNode>) : XamlNode
data class TextBlockNode(override val attributes: Map<String, String>, override val children: List<XamlNode>) : XamlNode
data class ButtonNode(override val attributes: Map<String, String>, override val children: List<XamlNode>) : XamlNode
data class TextButtonNode(override val attributes: Map<String, String>, override val children: List<XamlNode>) : XamlNode
data class IconTextButtonNode(override val attributes: Map<String, String>, override val children: List<XamlNode>) : XamlNode
data class IconButtonNode(override val attributes: Map<String, String>, override val children: List<XamlNode>) : XamlNode
data class PathNode(override val attributes: Map<String, String>, override val children: List<XamlNode>) : XamlNode
data class HintNode(override val attributes: Map<String, String>, override val children: List<XamlNode>) : XamlNode
data class ImageNode(override val attributes: Map<String, String>, override val children: List<XamlNode>) : XamlNode
data class ListItemNode(override val attributes: Map<String, String>, override val children: List<XamlNode>) : XamlNode
data class UnknownNode(val name: String, override val attributes: Map<String, String>, override val children: List<XamlNode>) : XamlNode
