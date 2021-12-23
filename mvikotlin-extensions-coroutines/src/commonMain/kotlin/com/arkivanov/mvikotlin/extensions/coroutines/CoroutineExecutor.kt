package com.arkivanov.mvikotlin.extensions.coroutines

import com.arkivanov.mvikotlin.core.annotations.MainThread
import com.arkivanov.mvikotlin.core.store.Bootstrapper
import com.arkivanov.mvikotlin.core.store.Executor
import com.arkivanov.mvikotlin.core.store.Executor.Callbacks
import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.utils.internal.atomic
import com.arkivanov.mvikotlin.utils.internal.initialize
import com.arkivanov.mvikotlin.utils.internal.requireValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlin.coroutines.CoroutineContext

/**
 * An abstract implementation of the [Executor] that exposes a [CoroutineScope] for coroutines launching.
 *
 * @param mainContext a [CoroutineContext] to be used by the exposed [CoroutineScope]
 */
open class CoroutineExecutor<in Intent : Any, in Action : Any, in State : Any, Result : Any, Label : Any>(
    mainContext: CoroutineContext = Dispatchers.Main
) : Executor<Intent, Action, State, Result, Label> {

    private val callbacks = atomic<Callbacks<State, Result, Label>>()
    private val getState: () -> State = { callbacks.requireValue().state }

    /**
     * A [CoroutineScope] that can be used by the [CoroutineExecutor] descendants to launch coroutines.
     * The [CoroutineScope] is automatically cancelled on dispose.
     */
    protected val scope: CoroutineScope = CoroutineScope(mainContext)

    final override fun init(callbacks: Callbacks<State, Result, Label>) {
        this.callbacks.initialize(callbacks)
    }

    final override fun handleIntent(intent: Intent) {
        executeIntent(intent, getState)
    }

    /**
     * Called by the [Store] for every received `Intent`
     *
     * @param intent an `Intent` received by the [Store]
     * @param getState a function that returns the current `State` of the [Store], must be called on Main thread
     */
    @MainThread
    protected open fun executeIntent(intent: Intent, @MainThread getState: () -> State) {
    }

    final override fun handleAction(action: Action) {
        executeAction(action, getState)
    }

    /**
     * Called by the [Store] for every `Action` produced by the [Bootstrapper]
     *
     * @param action an `Action` produced by the [Bootstrapper]
     * @param getState a function that returns the current `State` of the [Store], must be called on Main thread
     */
    @MainThread
    protected open fun executeAction(action: Action, @MainThread getState: () -> State) {
    }

    override fun dispose() {
        scope.cancel()
    }

    /**
     * Dispatches the provided `Result` to the [Reducer].
     * The updated `State` will be available immediately after this method returns.
     *
     * @param result a `Result` to be dispatched to the `Reducer`
     */
    @MainThread
    protected fun dispatch(result: Result) {
        callbacks.requireValue().onResult(result)
    }

    /**
     * Sends the provided `Label` to the [Store] for publication
     *
     * @param label a `Label` to be published
     */
    @MainThread
    protected fun publish(label: Label) {
        callbacks.requireValue().onLabel(label)
    }
}
