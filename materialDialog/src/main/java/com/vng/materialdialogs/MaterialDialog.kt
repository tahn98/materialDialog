/**
 * Designed and developed by Aidan Follestad (@afollestad)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package com.vng.materialdialogs

import android.app.Dialog
import android.content.Context
import android.graphics.Color.TRANSPARENT
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.util.TypedValue.COMPLEX_UNIT_DIP
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import androidx.annotation.*
import com.vng.materialdialogs.WhichButton.*
import com.vng.materialdialogs.actions.getActionButton
import com.vng.materialdialogs.callbacks.invokeAll
import com.vng.materialdialogs.consts.Theme
import com.vng.materialdialogs.internal.list.DialogAdapter
import com.vng.materialdialogs.internal.main.DialogLayout
import com.vng.materialdialogs.list.getListAdapter
import com.vng.materialdialogs.message.DialogMessageSettings
import com.vng.materialdialogs.utils.*
import com.vng.materialdialogs.utils.MDUtil.assertOneSet
import com.vng.materialdialogs.utils.MDUtil.resolveDimen
import com.vng.materialdialogs.utils.font
import com.vng.materialdialogs.utils.isVisible
import com.vng.materialdialogs.utils.preShow
import com.vng.materialdialogs.utils.resolveColor

typealias DialogCallback = (MaterialDialog) -> Unit

/** @author Aidan Follestad (afollestad) */
class MaterialDialog constructor(
    val windowContext: Context, dialogBehavior: DialogBehavior = DEFAULT_BEHAVIOR, theme: Theme? = null):
    Dialog(
        windowContext,
        if(theme != null) getTheme(dialogBehavior, theme = theme) else inferTheme(windowContext, dialogBehavior),
    ) {

        /**
     * A named config map, used like tags for extensions.
     *
     * Developers extending functionality of Material Dialogs should not use things
     * like static variables to store things. They instead should be stored at a dialog
     * instance level, which is what this provides.
     */
    val config: MutableMap<String, Any> = mutableMapOf()

    @Suppress("UNCHECKED_CAST")
    fun <T> config(key: String): T {
        return config[key] as T
    }

    var dialogBehavior: DialogBehavior = DEFAULT_BEHAVIOR
    /** Returns true if auto dismiss is enabled. */
    var autoDismissEnabled: Boolean = true
        internal set
    var titleFont: Typeface? = null
        internal set
    var bodyFont: Typeface? = null
        internal set
    var buttonFont: Typeface? = null
        internal set
    var cancelOnTouchOutside: Boolean = true
        internal set
    var cancelable: Boolean = true
        internal set
    var cornerRadius: Float? = null
        internal set
    var contextThemeWrapper: Context
        internal set
    @Px
    private var maxWidth: Int? = null

    /** The root layout of the dialog. */
    val view: DialogLayout

    internal val preShowListeners = mutableListOf<DialogCallback>()
    internal val showListeners = mutableListOf<DialogCallback>()
    internal val dismissListeners = mutableListOf<DialogCallback>()
    internal val cancelListeners = mutableListOf<DialogCallback>()

    private val positiveListeners = mutableListOf<DialogCallback>()
    private val negativeListeners = mutableListOf<DialogCallback>()
    private val neutralListeners = mutableListOf<DialogCallback>()

    init {
        contextThemeWrapper = if(theme == null){
            windowContext
        }else{
            ContextThemeWrapper(windowContext, getTheme(dialogBehavior, theme))
        }

        val layoutInflater = LayoutInflater.from(contextThemeWrapper)
        val rootView = dialogBehavior.createView(
            creatingContext = contextThemeWrapper,
            dialogWindow = window!!,
            layoutInflater = layoutInflater,
            dialog = this
        )
        setContentView(rootView)
        this.view = dialogBehavior.getDialogLayout(rootView)
            .also { it.attachDialog(this) }

        // Set defaults
        this.titleFont = font(attr = R.attr.md_font_title)
        this.bodyFont = font(attr = R.attr.md_font_body)
        this.buttonFont = font(attr = R.attr.md_font_button)
        invalidateBackgroundColorAndRadius()
    }


    /**
     * Shows an drawable to the left of the dialog title.
     *
     * @param res The drawable resource to display as the drawable.
     * @param drawable The drawable to display as the drawable.
     */
    fun icon(
        @DrawableRes res: Int? = null,
        drawable: Drawable? = null
    ): MaterialDialog = apply {
        assertOneSet("icon", drawable, res)
        populateIcon(
            view.titleLayout.iconView,
            iconRes = res,
            icon = drawable
        )
    }

    /**
     * Shows a title, or header, at the top of the dialog.
     *
     * @param res The string resource to display as the title.
     * @param text The literal string to display as the title.
     */
    fun title(
        @StringRes res: Int? = null,
        text: String? = null,
        textColor: Int? = null
    ): MaterialDialog = apply {
        assertOneSet("title", text, res)
        populateText(
            view.titleLayout.titleView,
            textRes = res,
            text = text,
            typeface = this.titleFont,
            textColor = textColor ?: R.attr.md_color_title
        )
    }

    /**
     * Shows a message, below the title, and above the action buttons (and checkbox prompt).
     *
     * @param res The string resource to display as the message.
     * @param text The literal string to display as the message.
     */
    fun message(
        @StringRes res: Int? = null,
        text: CharSequence? = null,
        textColor: Int? = null,
        applySettings: (DialogMessageSettings.() -> Unit)? = null
    ): MaterialDialog = apply {
        assertOneSet("message", text, res)
        this.view.contentLayout.setMessage(
            dialog = this,
            res = res,
            text = text,
            textColor = textColor,
            typeface = this.bodyFont,
            applySettings = applySettings
        )
    }

    /**
     * Shows a positive action button, in the far right at the bottom of the dialog.
     *
     * @param res The string resource to display on the title.
     * @param text The literal string to display on the button.
     * @param click A listener to invoke when the button is pressed.
     */
    fun positiveButton(
        @StringRes res: Int? = null,
        text: CharSequence? = null,
        textColor: Int? = null,
        click: DialogCallback? = null
    ): MaterialDialog = apply {
        if (click != null) {
            positiveListeners.add(click)
        }

        val btn = getActionButton(POSITIVE)
        if (res == null && text == null && btn.isVisible()) {
            // Didn't receive text and the button is already setup,
            // so just stop with the added listener.
            return this
        }

        populateText(
            btn,
            textRes = res,
            text = text,
            fallback = android.R.string.ok,
            typeface = this.buttonFont,
            textColor = textColor
        )
    }

    /** Clears any positive action button listeners set via usages of [positiveButton]. */
    fun clearPositiveListeners(): MaterialDialog = apply {
        this.positiveListeners.clear()
    }

    /**
     * Shows a negative action button, to the left of the positive action button (or at the far
     * right if there is no positive action button).
     *
     * @param res The string resource to display on the title.
     * @param text The literal string to display on the button.
     * @param click A listener to invoke when the button is pressed.
     */
    fun negativeButton(
        @StringRes res: Int? = null,
        text: CharSequence? = null,
        click: DialogCallback? = null,
        textColor: Int? = null,
    ): MaterialDialog = apply {
        if (click != null) {
            negativeListeners.add(click)
        }

        val btn = getActionButton(NEGATIVE)
        if (res == null && text == null && btn.isVisible()) {
            // Didn't receive text and the button is already setup,
            // so just stop with the added listener.
            return@apply
        }

        populateText(
            btn,
            textRes = res,
            text = text,
            fallback = android.R.string.cancel,
            typeface = this.buttonFont,
            textColor = textColor
        )
    }

    /** Clears any negative action button listeners set via usages of [negativeButton]. */
    fun clearNegativeListeners(): MaterialDialog = apply {
        this.negativeListeners.clear()
    }

    @Deprecated(
        "Use of neutral buttons is discouraged, see " +
                "https://material.io/design/components/dialogs.html#actions."
    )
    fun neutralButton(
        @StringRes res: Int? = null,
        text: CharSequence? = null,
        click: DialogCallback? = null,
        textColor: Int? = null
    ): MaterialDialog = apply {
        if (click != null) {
            neutralListeners.add(click)
        }

        val btn = getActionButton(NEUTRAL)
        if (res == null && text == null && btn.isVisible()) {
            // Didn't receive text and the button is already setup,
            // so just stop with the added listener.
            return@apply
        }

        populateText(
            btn,
            textRes = res,
            text = text,
            typeface = this.buttonFont,
            textColor = textColor
        )
    }

    @Deprecated(
        "Use of neutral buttons is discouraged, see " +
                "https://material.io/design/components/dialogs.html#actions."
    )
    fun clearNeutralListeners(): MaterialDialog = apply {
        this.neutralListeners.clear()
    }

    /**
     * Turns off auto dismiss. Action button and list item clicks won't dismiss the dialog on their
     * own. You have to handle dismissing the dialog manually with the [dismiss] method.
     */
    @CheckResult
    fun noAutoDismiss(): MaterialDialog = apply {
        this.autoDismissEnabled = false
    }

    /**
     * Be careful with this. The specs say to use 280dp on a phone, and this value increases
     * for landscape, tablets. etc.
     *
     * If you override this, you should make sure you test on larger screens and in different
     * orientations.
     *
     * This value only takes effect when calling [show].
     */
    fun maxWidth(
        @DimenRes res: Int? = null,
        @Px literal: Int? = null
    ): MaterialDialog = apply {
        assertOneSet("maxWidth", res, literal)
        val shouldSetConstraints = this.maxWidth != null && this.maxWidth == 0
        this.maxWidth = if (res != null) {
            windowContext.resources.getDimensionPixelSize(res)
        } else {
            literal!!
        }
        if (shouldSetConstraints) {
            setWindowConstraints()
        }
    }

    /**
     * Sets the corner radius for the dialog, or the rounding of the corners. Dialogs can choose
     * how they want to handle this, e.g. bottom sheets will only round the top left and right
     * corners.
     */
    fun cornerRadius(
        literalDp: Float? = null,
        @DimenRes res: Int? = null
    ): MaterialDialog = apply {
        assertOneSet("cornerRadius", literalDp, res)
        this.cornerRadius = if (res != null) {
            windowContext.resources.getDimension(res)
        } else {
            val displayMetrics = windowContext.resources.displayMetrics
            TypedValue.applyDimension(COMPLEX_UNIT_DIP, literalDp!!, displayMetrics)
        }
        invalidateBackgroundColorAndRadius()
    }

    /** Turns debug mode on or off. Draws spec guides over dialog views. */
    @CheckResult
    fun debugMode(
        debugMode: Boolean = true
    ): MaterialDialog = apply {
        this.view.debugMode = debugMode
    }

    /** Opens the dialog. */
    override fun show() {
        setWindowConstraints()
        preShow()
        dialogBehavior.onPreShow(this)
        super.show()
        dialogBehavior.onPostShow(this)
    }

    /** Applies multiple properties to the dialog and opens it. */
    inline fun show(func: MaterialDialog.() -> Unit): MaterialDialog = apply {
        this.func()
        this.show()
    }

    /** Configures whether or not the dialog can be cancelled. */
    fun cancelable(cancelable: Boolean): MaterialDialog = apply {
        @Suppress("DEPRECATION")
        setCancelable(cancelable)
    }

    @Deprecated(
        message = "Use fluent cancelable(Boolean) instead.",
        replaceWith = ReplaceWith("cancelable(cancelable)")
    )
    override fun setCancelable(cancelable: Boolean) {
        this.cancelable = cancelable
        super.setCancelable(cancelable)
    }

    /** Whether or not touching outside of the dialog UI will cancel the dialog. */
    fun cancelOnTouchOutside(cancelable: Boolean): MaterialDialog = apply {
        @Suppress("DEPRECATION")
        setCanceledOnTouchOutside(cancelable)
    }

    @Deprecated(
        message = "Use fluent cancelOnTouchOutside(Boolean) instead.",
        replaceWith = ReplaceWith("cancelOnTouchOutside(cancelOnTouchOutside)")
    )
    override fun setCanceledOnTouchOutside(cancelOnTouchOutside: Boolean) {
        this.cancelOnTouchOutside = cancelOnTouchOutside
        super.setCanceledOnTouchOutside(cancelOnTouchOutside)
    }

    override fun dismiss() {
        if (dialogBehavior.onDismiss()) return
        hideKeyboard()
        super.dismiss()
    }

    internal fun onActionButtonClicked(which: WhichButton) {
        when (which) {
            POSITIVE -> {
                positiveListeners.invokeAll(this)
                val adapter = getListAdapter() as? DialogAdapter<*, *>
                adapter?.positiveButtonClicked()
            }
            NEGATIVE -> negativeListeners.invokeAll(this)
            NEUTRAL -> neutralListeners.invokeAll(this)
        }
        if (autoDismissEnabled) {
            dismiss()
        }
    }

    private fun setWindowConstraints() {
        dialogBehavior.setWindowConstraints(
            context = windowContext,
            maxWidth = maxWidth,
            window = window!!,
            view = view
        )
    }

    fun backgroundColor(backgroundColor: Int): MaterialDialog {
        window?.setBackgroundDrawable(ColorDrawable(TRANSPARENT))
        dialogBehavior.setBackgroundColor(
            view = view,
            color = backgroundColor,
            cornerRadius = cornerRadius ?: resolveDimen(
                windowContext,
                attr = R.attr.md_corner_radius
            ) {
                windowContext.resources.getDimension(R.dimen.md_dialog_default_corner_radius)
            }
        )
        return this
    }

    private fun invalidateBackgroundColorAndRadius() {
        val backgroundColor = resolveColor(
            attr = R.attr.md_background_color
        ) {
            resolveColor(attr = R.attr.colorBackgroundFloating
            )
        }
        window?.setBackgroundDrawable(ColorDrawable(TRANSPARENT))
        dialogBehavior.setBackgroundColor(
            view = view,
            color = backgroundColor,
            cornerRadius = cornerRadius ?: resolveDimen(
                windowContext,
                attr = R.attr.md_corner_radius
            ) {
                windowContext.resources.getDimension(R.dimen.md_dialog_default_corner_radius)
            }
        )
    }

    companion object {
        /**
         * The default [dialogBehavior] for all constructed instances of
         * [MaterialDialog]. Defaults to [ModalDialog].
         */
        @JvmStatic
        var DEFAULT_BEHAVIOR: DialogBehavior = ModalDialog
    }
}
