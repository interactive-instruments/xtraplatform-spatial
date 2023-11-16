/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain.transform;

import de.ii.xtraplatform.features.domain.FeatureEventHandler;
import de.ii.xtraplatform.features.domain.FeatureEventHandler.ModifiableContext;
import de.ii.xtraplatform.features.domain.FeatureTokenEmitter2;
import de.ii.xtraplatform.features.domain.FeatureTokenReader;
import de.ii.xtraplatform.features.domain.FeatureTokenType;
import de.ii.xtraplatform.features.domain.SchemaBase;
import de.ii.xtraplatform.features.domain.SchemaMapping;
import de.ii.xtraplatform.features.domain.SchemaMappingBase;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Vector;

public class FeatureEventBuffer<
        U extends SchemaBase<U>, V extends SchemaMappingBase<U>, W extends ModifiableContext<U, V>>
    implements FeatureTokenEmitter2<U, V, W> {

  private final FeatureEventHandler<U, V, W> downstream;
  private final List<Object> buffer;
  private final FeatureTokenEmitter2<U, V, W> bufferIn;
  private final FeatureTokenReader<U, V, W> bufferOut;

  private final Vector<Integer> events;
  private boolean doBuffer;
  public int current;
  public List<Integer> currentEnclosing;

  public FeatureEventBuffer(
      FeatureEventHandler<U, V, W> downstream, W context, Map<String, SchemaMapping> mappings) {
    this.downstream = downstream;
    this.buffer = new ArrayList<>();
    this.bufferIn = (FeatureTokenEmitter2<U, V, W>) (this::append);
    this.bufferOut = new FeatureTokenReader<>(downstream, context);
    this.events = new Vector<>();

    this.doBuffer = false;
    this.current = 0;
    this.currentEnclosing = List.of();

    int maxEvents =
        mappings.values().stream()
                    .mapToInt(SchemaMappingBase::getNumberOfTargets)
                    .max()
                    .orElseThrow()
                * 2
            + 2;
    events.setSize(maxEvents);
  }

  public FeatureTokenEmitter2<U, V, W> getBuffer() {
    return bufferIn;
  }

  public void next(int pos) {
    next(pos, List.of());
  }

  public void next(int pos, List<Integer> enclosing) {
    this.current = pos;
    this.currentEnclosing = enclosing;
  }

  /**
   * An event consists of 1 to n tokens and is saved in the buffer. An event has a desired position
   * that must not match the order of occurrence. events contains the buffer start index and token
   * count for every event by position.
   *
   * @param pos event position
   * @return first index for event position in buffer
   */
  private int start(int pos) {
    return events.get(pos * 2);
  }

  /**
   * @param pos event position
   * @return length for event position in buffer
   */
  private int length(int pos) {
    return events.get((pos * 2) + 1);
  }

  /**
   * @param pos event position
   * @return last index for event position in buffer
   */
  private int end(int pos) {
    return start(pos) + length(pos);
  }

  /**
   * Increase length for given event position in buffer.
   *
   * @param pos event position
   */
  private void increase(int pos) {
    plus(pos, 1);
  }

  private void plus(int pos, int delta) {
    // increase length of pos
    int lenPos = (pos * 2) + 1;
    events.set(lenPos, events.get(lenPos) + delta);

    // increase start of following pos
    for (int i = (pos + 1) * 2; i < events.size(); i += 2) {
      events.set(i, events.get(i) + delta);
    }
  }

  /**
   * Positions in enclosing are always smaller than pos, the last position in enclosing is the
   * smallest.
   *
   * @param pos
   * @param enclosing
   * @return
   */
  private int minPos(int pos, List<Integer> enclosing) {
    if (enclosing.isEmpty()) {
      return pos;
    }
    return enclosing.get(enclosing.size() - 1);
  }

  void append(Object token) {
    int end = end(current);
    buffer.add(end, token);

    int minPos = minPos(current, currentEnclosing);

    increase(minPos);
  }

  void reset() {
    Collections.fill(events, 0);
  }

  public void bufferStart() {
    this.doBuffer = true;
  }

  public void bufferStop(boolean flush) {
    this.doBuffer = false;
    if (flush) {
      bufferFlush();
    }
  }

  public void bufferFlush() {
    buffer.add(FeatureTokenType.FLUSH);
    buffer.forEach(bufferOut::onToken);
    buffer.clear();
  }

  public boolean isBuffering() {
    return doBuffer;
  }

  public List<Object> getSlice(int pos) {
    if (pos < 1) {
      return List.of();
    }

    return Collections.unmodifiableList(buffer.subList(start(pos), end(pos)));
  }

  public boolean replaceSlice(int pos, List<Object> replacement) {
    if (pos < 1) {
      return false;
    }

    List<Object> slice = buffer.subList(start(pos), end(pos));

    if (Objects.equals(slice, replacement)) {
      return false;
    }

    int delta = replacement.size() - slice.size();

    slice.clear();
    slice.addAll(replacement);

    if (delta != 0) {
      plus(pos, delta);
    }

    return true;
  }

  @Override
  public void push(Object token) {
    append(token);
  }

  @Override
  public void onStart(W context) {
    if (doBuffer) {
      bufferIn.onStart(context);
    } else {
      downstream.onStart(context);
    }
  }

  @Override
  public void onEnd(W context) {
    if (doBuffer) {
      bufferIn.onEnd(context);
    } else {
      downstream.onEnd(context);
    }
  }

  @Override
  public void onFeatureStart(W context) {
    reset();
    if (doBuffer) {
      bufferIn.onFeatureStart(context);
    } else {
      downstream.onFeatureStart(context);
    }
  }

  @Override
  public void onFeatureEnd(W context) {
    if (doBuffer) {
      bufferIn.onFeatureEnd(context);
    } else {
      downstream.onFeatureEnd(context);
    }
  }

  @Override
  public void onObjectStart(W context) {
    if (doBuffer) {
      bufferIn.onObjectStart(context);
    } else {
      downstream.onObjectStart(context);
    }
  }

  @Override
  public void onObjectEnd(W context) {
    if (doBuffer) {
      bufferIn.onObjectEnd(context);
    } else {
      downstream.onObjectEnd(context);
    }
  }

  @Override
  public void onArrayStart(W context) {
    if (doBuffer) {
      bufferIn.onArrayStart(context);
    } else {
      downstream.onArrayStart(context);
    }
  }

  @Override
  public void onArrayEnd(W context) {
    if (doBuffer) {
      bufferIn.onArrayEnd(context);
    } else {
      downstream.onArrayEnd(context);
    }
  }

  @Override
  public void onValue(W context) {
    if (doBuffer) {
      bufferIn.onValue(context);
    } else {
      downstream.onValue(context);
    }
  }
}
