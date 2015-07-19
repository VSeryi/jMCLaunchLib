package uk.co.rx14.jmclaunchlib.util

import groovy.transform.Canonical
import groovy.transform.ToString

import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Logger

/**
 * A task that can be competed, has a relative weight and subtasks.
 * Used for tracking progress.
 * <p>
 * Total weight of this task is this task's weight + subtasks weight. Subtasks are counted recursively.
 */
@Canonical
@ToString(includePackage = false, includeNames = true)
trait Task {

	private static final Logger LOGGER = Logger.getLogger(Task.class.getName())

	abstract List<Task> getSubtasks()

	abstract int getWeight()

	abstract void before()
	abstract void after()

	private AtomicBoolean started = new AtomicBoolean()
	private boolean done

	void start() {
		if (!started.compareAndSet(false, true)) return

		LOGGER.finer "[${Thread.currentThread().name}] ${getClass()}::before enter"
		before()
		LOGGER.finer "[${Thread.currentThread().name}] ${getClass()}::before exit"

		if (subtasks.size() > 0 ) {
			LOGGER.finest "[${Thread.currentThread().name}] ${getClass()} starting in parallel: $subtasks"
			subtasks.parallelStream().forEach { it.start() }
		}

		LOGGER.finer "[${Thread.currentThread().name}] ${getClass()}::after enter"
		after()
		LOGGER.finer "[${Thread.currentThread().name}] ${getClass()}::after exit"

		done = true
	}

	int getTotalWeight() {
		weight + subtasks.totalWeight.sum(0)
	}

	int getCompletedWeight() {
		(done ? weight : 0) + subtasks.completedWeight.sum(0)
	}

	double getCompletedPercentage() {
		completedWeight / totalWeight * 100d
	}

	List<Task> getDoneTasks() {
		def tasks = subtasks.doneTasks.flatten()
		if (done) {
			tasks += this
		}
		tasks
	}

	List<Task> getCurrentTasks() {
		def tasks = subtasks.currentTasks.flatten()
		if (started && !done) {
			tasks += this
		}
		tasks
	}

	List<Task> getRemainingTasks() {
		def tasks = subtasks.remainingTasks.flatten()
		if (!started && !done) {
			tasks += this
		}
		tasks
	}

	boolean isStarted() { started }

	boolean isDone() { done }
}