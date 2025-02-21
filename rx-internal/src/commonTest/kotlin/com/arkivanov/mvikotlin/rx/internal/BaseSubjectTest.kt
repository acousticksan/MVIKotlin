package com.arkivanov.mvikotlin.rx.internal

import com.arkivanov.mvikotlin.rx.observer
import com.arkivanov.mvikotlin.utils.internal.isAssertOnMainThreadEnabled
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BaseSubjectTest {

    private val subject = BaseSubject<Int?>()

    @BeforeTest
    fun before() {
        isAssertOnMainThreadEnabled = false
    }

    @AfterTest
    fun after() {
        isAssertOnMainThreadEnabled = true
    }

    @Test
    fun multicasts_values_to_all_subscribers() {
        val values1 = ArrayList<Int?>()
        val values2 = ArrayList<Int?>()

        subject.subscribe(observer(onNext = { values1 += it }))
        subject.onNext(0)
        subject.onNext(null)
        subject.subscribe(observer(onNext = { values2 += it }))
        subject.onNext(1)
        subject.onNext(null)

        assertEquals(listOf(0, null, 1, null), values1)
        assertEquals(listOf(1, null), values2)
    }

    @Test
    fun completes_all_existing_observers_WHEN_completed() {
        var isCompleted1 = false
        var isCompleted2 = false

        subject.subscribe(observer = observer(onComplete = {
            isCompleted1 = true
        }))
        subject.onNext(0)
        subject.subscribe(observer = observer(onComplete = {
            isCompleted2 = true
        }))
        subject.onNext(1)
        subject.onComplete()

        assertTrue(isCompleted1)
        assertTrue(isCompleted2)
    }

    @Test
    fun completes_new_observer_WHEN_already_completed_and_new_observer_subscribed() {
        var isCompleted = false

        subject.onComplete()
        subject.subscribe(observer = observer(onComplete = {
            isCompleted = true
        }))

        assertTrue(isCompleted)
    }

    @Test
    fun does_not_produce_values_to_unsubscribed_observers() {
        var hasValue = false

        subject.subscribe(observer { hasValue = true }).dispose()
        subject.onNext(0)

        assertFalse(hasValue)
    }

    @Test
    fun produces_values_to_another_observers_WHEN_one_observer_unsubscribed_and_new_values() {
        val disposable1 = subject.subscribe(observer())
        val values2 = ArrayList<Int?>()

        subject.subscribe(observer(onNext = { values2 += it }))
        disposable1.dispose()
        subject.onNext(0)
        subject.onNext(null)

        assertEquals(listOf(0, null), values2)
    }

    @Test
    fun disposables_are_disposed_WHEN_completed() {
        val disposable1 = subject.subscribe(observer())
        val disposable2 = subject.subscribe(observer())

        subject.onComplete()

        assertTrue(disposable1.isDisposed)
        assertTrue(disposable2.isDisposed)
    }

    @Test
    fun disposable_is_disposed_WHEN_already_completed_and_new_observer_subscribed() {
        subject.onComplete()
        val disposable = subject.subscribe(observer())

        assertTrue(disposable.isDisposed)
    }

    @Test
    fun does_not_emit_values_recursively() {
        var isEmitting = false
        var isEmittedRecursively = false

        subject.subscribe(
            observer { value ->
                if (value == 1) {
                    isEmitting = true
                    subject.onNext(2)
                    isEmitting = false
                } else {
                    isEmittedRecursively = isEmitting
                }
            }
        )

        subject.onNext(1)

        assertFalse(isEmittedRecursively)
    }

    @Test
    fun emits_all_values_in_order_WHEN_onNext_called_recursively() {
        val values = ArrayList<Int?>()

        subject.subscribe(
            observer { value ->
                if (value == 1) {
                    subject.onNext(null)
                    subject.onNext(2)
                }
                values += value
            }
        )

        subject.onNext(1)

        assertEquals(listOf(1, null, 2), values)
    }

    @Test
    fun does_not_complete_recursively() {
        var isEmitting = false
        var isCompletedRecursively = false

        subject.subscribe(
            observer(
                onComplete = { isCompletedRecursively = isEmitting },
                onNext = {
                    isEmitting = true
                    subject.onComplete()
                    isEmitting = false
                }
            )
        )

        subject.onNext(1)

        assertFalse(isCompletedRecursively)
    }

    @Test
    fun completes_WHEN_onComplete_called_recursively() {
        var isCompleted = false

        subject.subscribe(
            observer(
                onComplete = { isCompleted = true },
                onNext = { subject.onComplete() }
            )
        )

        subject.onNext(1)

        assertTrue(isCompleted)
    }
}
