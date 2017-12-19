/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.exoplayer2;

import android.os.Handler;
import android.support.annotation.Nullable;
import com.google.android.exoplayer2.util.Assertions;

/**
 * Defines a player message which can be sent with a {@link Sender} and received by a {@link
 * Target}.
 */
public final class PlayerMessage {

  /** A target for messages. */
  public interface Target {

    /**
     * Handles a message delivered to the target.
     *
     * @param messageType The message type.
     * @param message The message.
     * @throws ExoPlaybackException If an error occurred whilst handling the message.
     */
    void handleMessage(int messageType, Object message) throws ExoPlaybackException;
  }

  /** A sender for messages. */
  public interface Sender {

    /** A listener for message events triggered by the sender. */
    interface Listener {

      /** Called when the message has been delivered. */
      void onMessageDelivered();

      /** Called when the message has been deleted. */
      void onMessageDeleted();
    }

    /**
     * Sends a message.
     *
     * @param message The message to be sent.
     * @param listener The listener to listen to message events.
     */
    void sendMessage(PlayerMessage message, Listener listener);
  }

  private final Target target;
  private final Sender sender;
  private final Timeline timeline;

  private int type;
  private Object message;
  private Handler handler;
  private int windowIndex;
  private long positionMs;
  private boolean deleteAfterDelivery;
  private boolean isSent;
  private boolean isDelivered;
  private boolean isDeleted;

  /**
   * Creates a new message.
   *
   * @param sender The {@link Sender} used to send the message.
   * @param target The {@link Target} the message is sent to.
   * @param timeline The timeline used when setting the position with {@link #setPosition(long)}. If
   *     set to {@link Timeline#EMPTY}, any position can be specified.
   * @param defaultWindowIndex The default window index in the {@code timeline} when no other window
   *     index is specified.
   * @param defaultHandler The default handler to send the message on when no other handler is
   *     specified.
   */
  public PlayerMessage(
      Sender sender,
      Target target,
      Timeline timeline,
      int defaultWindowIndex,
      Handler defaultHandler) {
    this.sender = sender;
    this.target = target;
    this.timeline = timeline;
    this.handler = defaultHandler;
    this.windowIndex = defaultWindowIndex;
    this.positionMs = C.TIME_UNSET;
    this.deleteAfterDelivery = true;
  }

  /** Returns the timeline used for setting the position with {@link #setPosition(long)}. */
  public Timeline getTimeline() {
    return timeline;
  }

  /** Returns the target the message is sent to. */
  public Target getTarget() {
    return target;
  }

  /**
   * Sets a custom message type forwarded to the {@link Target#handleMessage(int, Object)}.
   *
   * @param messageType The custom message type.
   * @return This message.
   * @throws IllegalStateException If {@link #send()} has already been called.
   */
  public PlayerMessage setType(int messageType) {
    Assertions.checkState(!isSent);
    this.type = messageType;
    return this;
  }

  /** Returns custom message type forwarded to the {@link Target#handleMessage(int, Object)}. */
  public int getType() {
    return type;
  }

  /**
   * Sets a custom message forwarded to the {@link Target#handleMessage(int, Object)}.
   *
   * @param message The custom message.
   * @return This message.
   * @throws IllegalStateException If {@link #send()} has already been called.
   */
  public PlayerMessage setMessage(@Nullable Object message) {
    Assertions.checkState(!isSent);
    this.message = message;
    return this;
  }

  /** Returns custom message forwarded to the {@link Target#handleMessage(int, Object)}. */
  public Object getMessage() {
    return message;
  }

  /**
   * Sets the handler the message is delivered on.
   *
   * @param handler A {@link Handler}.
   * @return This message.
   * @throws IllegalStateException If {@link #send()} has already been called.
   */
  public PlayerMessage setHandler(Handler handler) {
    Assertions.checkState(!isSent);
    this.handler = handler;
    return this;
  }

  /** Returns the handler the message is delivered on. */
  public Handler getHandler() {
    return handler;
  }

  /**
   * Sets a position in the current window at which the message will be delivered.
   *
   * @param positionMs The position in the current window at which the message will be sent, in
   *     milliseconds.
   * @return This message.
   * @throws IllegalStateException If {@link #send()} has already been called.
   */
  public PlayerMessage setPosition(long positionMs) {
    Assertions.checkState(!isSent);
    this.positionMs = positionMs;
    return this;
  }

  /**
   * Returns position in window at {@link #getWindowIndex()} at which the message will be delivered,
   * in milliseconds. If {@link C#TIME_UNSET}, the message will be delivered immediately.
   */
  public long getPositionMs() {
    return positionMs;
  }

  /**
   * Sets a position in a window at which the message will be delivered.
   *
   * @param windowIndex The index of the window at which the message will be sent.
   * @param positionMs The position in the window with index {@code windowIndex} at which the
   *     message will be sent, in milliseconds.
   * @return This message.
   * @throws IllegalSeekPositionException If the timeline returned by {@link #getTimeline()} is not
   *     empty and the provided window index is not within the bounds of the timeline.
   * @throws IllegalStateException If {@link #send()} has already been called.
   */
  public PlayerMessage setPosition(int windowIndex, long positionMs) {
    Assertions.checkState(!isSent);
    Assertions.checkArgument(positionMs != C.TIME_UNSET);
    if (windowIndex < 0 || (!timeline.isEmpty() && windowIndex >= timeline.getWindowCount())) {
      throw new IllegalSeekPositionException(timeline, windowIndex, positionMs);
    }
    this.windowIndex = windowIndex;
    this.positionMs = positionMs;
    return this;
  }

  /** Returns window index at which the message will be delivered. */
  public int getWindowIndex() {
    return windowIndex;
  }

  /**
   * Sets whether the message will be deleted after delivery. If false, the message will be resent
   * if playback reaches the specified position again. Only allowed to be false if a position is set
   * with {@link #setPosition(long)}.
   *
   * @param deleteAfterDelivery Whether the message is deleted after delivery.
   * @return This message.
   * @throws IllegalStateException If {@link #send()} has already been called.
   */
  public PlayerMessage setDeleteAfterDelivery(boolean deleteAfterDelivery) {
    Assertions.checkState(!isSent);
    this.deleteAfterDelivery = deleteAfterDelivery;
    return this;
  }

  /** Returns whether the message will be deleted after delivery. */
  public boolean getDeleteAfterDelivery() {
    return deleteAfterDelivery;
  }

  /**
   * Sends the message. If the target throws an {@link ExoPlaybackException} then it is propagated
   * out of the player as an error using {@link
   * Player.EventListener#onPlayerError(ExoPlaybackException)}.
   *
   * @return This message.
   * @throws IllegalStateException If {@link #send()} has already been called.
   */
  public PlayerMessage send() {
    Assertions.checkState(!isSent);
    if (positionMs == C.TIME_UNSET) {
      Assertions.checkArgument(deleteAfterDelivery);
    }
    isSent = true;
    sender.sendMessage(
        this,
        new Sender.Listener() {
          @Override
          public void onMessageDelivered() {
            synchronized (PlayerMessage.this) {
              isDelivered = true;
              PlayerMessage.this.notifyAll();
            }
          }

          @Override
          public void onMessageDeleted() {
            synchronized (PlayerMessage.this) {
              isDeleted = true;
              PlayerMessage.this.notifyAll();
            }
          }
        });
    return this;
  }

  /**
   * Blocks until after the message has been delivered or the player is no longer able to deliver
   * the message.
   *
   * <p>Note that this method can't be called if the current thread is the same thread used by the
   * message handler set with {@link #setHandler(Handler)} as it would cause a deadlock.
   *
   * @return Whether the message was delivered successfully.
   * @throws IllegalStateException If this method is called before {@link #send()}.
   * @throws IllegalStateException If this method is called on the same thread used by the message
   *     handler set with {@link #setHandler(Handler)}.
   * @throws InterruptedException If the current thread is interrupted while waiting for the message
   *     to be delivered.
   */
  public synchronized boolean blockUntilDelivered() throws InterruptedException {
    Assertions.checkState(!isSent);
    Assertions.checkState(handler.getLooper().getThread() != Thread.currentThread());
    while (!isDelivered && !isDeleted) {
      wait();
    }
    return isDelivered;
  }
}
