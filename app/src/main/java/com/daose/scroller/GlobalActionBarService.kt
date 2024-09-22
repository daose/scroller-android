package com.daose.scroller

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.FrameLayout


class GlobalActionBarService : AccessibilityService() {
    private lateinit var layout: FrameLayout
    private lateinit var scrollButton: Button
    private var speedMs = 300L;
    private val SPEED_INTERVAL = 100L;
    private val initial_delay = 1500L;
    private var delay_interval = initial_delay;

    override fun onServiceConnected() {
        super.onServiceConnected()

        Log.d(this.packageName, "service started")
        val wm: WindowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        layout = FrameLayout(this)
        val lp = WindowManager.LayoutParams()
        lp.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        lp.format = PixelFormat.TRANSLUCENT
        lp.flags = lp.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        lp.width = WindowManager.LayoutParams.WRAP_CONTENT
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT
        lp.gravity = Gravity.TOP
        val inflater = LayoutInflater.from(this)
        inflater.inflate(R.layout.action_bar, layout)
        wm.addView(layout, lp)

        configureScrollButton()
        configureSpeedButton(true);
        configureSpeedButton(false);
        configureDelayButton()
    }

    private fun configureDelayButton() {
        val button = layout.findViewById(R.id.delay) as Button
        button.setOnClickListener {
            delay_interval = delay_interval xor initial_delay
            scrollButton.performClick()
        }
    }

    private fun configureSpeedButton(speedUp: Boolean) {
        val speedButton = if (speedUp) {
            layout.findViewById(R.id.speed_up) as Button
        } else {
            layout.findViewById(R.id.speed_down) as Button
        }
        if (speedUp) {
            speedButton.setOnClickListener {
                speedMs = (speedMs - SPEED_INTERVAL).coerceAtLeast(1)
                scrollButton.performClick()
            }
        } else {
            speedButton.setOnClickListener {
                speedMs += SPEED_INTERVAL;
                scrollButton.performClick()
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    }

    override fun onInterrupt() {
    }

    private fun findScrollableNode(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val deque: ArrayDeque<AccessibilityNodeInfo> = ArrayDeque()
        deque.add(root)
        while (!deque.isEmpty()) {
            val node = deque.removeFirst()
            if (node.actionList.contains(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD)) {
                return node
            }
            for (i in 0 until node.childCount) {
                deque.addLast(node.getChild(i))
            }
        }
        return null
    }

    class ScrollCallback(private val globalActionBarService: GlobalActionBarService) :
        GestureResultCallback() {
        override fun onCompleted(gestureDescription: GestureDescription?) {
            if (gestureDescription != null) {
                val gestureBuilder = GestureDescription.Builder()
                gestureBuilder.addStroke(
                    GestureDescription.StrokeDescription(
                        gestureDescription.getStroke(
                            0
                        ).path,
                        globalActionBarService.delay_interval,
                        globalActionBarService.speedMs
                    )
                )
                globalActionBarService.dispatchGesture(gestureBuilder.build(), this, null)
                // globalActionBarService.dispatchGesture(gestureDescription, this, null)
            };
        }
    }

    private fun configureScrollButton() {
        scrollButton = layout.findViewById(R.id.scroll) as Button
        scrollButton.setOnClickListener {

            val scrollable = findScrollableNode(rootInActiveWindow)

//            val arguments = Bundle()
//            arguments.putInt(
//                AccessibilityNodeInfo.ACTION_SCROLL_FORWARD,
//                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER
//            )
//            scrollable?.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD.id)

            val scrollableBounds = Rect()
            scrollable?.getBoundsInScreen(scrollableBounds)
            val swipePath = Path()
            swipePath.moveTo(
                scrollableBounds.exactCenterX(),
                (scrollableBounds.bottom - 100).coerceAtLeast(scrollableBounds.centerY() + 1).toFloat()
            )
            swipePath.lineTo(scrollableBounds.exactCenterX(), scrollableBounds.exactCenterY())
            val gestureBuilder = GestureDescription.Builder()
            gestureBuilder.addStroke(GestureDescription.StrokeDescription(swipePath, 0, speedMs))
            dispatchGesture(gestureBuilder.build(), ScrollCallback(this), null)
        }
    }


}