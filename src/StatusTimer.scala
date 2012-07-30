package com.paulasmuth.fyrehose

import java.util.concurrent._
import java.lang.Runnable

class StatusTimer(){

  var last_time = FyrehoseUtil.now_ms
  var last_count = 0

  private val callback = new Runnable{
    def run = print_stats
  }

  private val scheduler = Executors.newSingleThreadScheduledExecutor()
  scheduler.scheduleAtFixedRate(callback, 0, 1500, TimeUnit.MILLISECONDS)

  def print_stats() = {
    val tdiff = FyrehoseUtil.now_ms - last_time
    val cdiff = Fyrehose.backbone.msg_total.get - last_count
    val msgps = cdiff / (tdiff / 1000.0)

    val conns = if (Fyrehose.tcp_listener == null) 0
                else Fyrehose.tcp_listener.num_connections.get

    Fyrehose.log("%.1f msg/s, %d conns, %d qrys, cache: %d/%d (%.1f MB)".format(
      msgps, conns,
      Fyrehose.backbone.queries.size,
      Fyrehose.message_cache.size,
      Fyrehose.MESSAGE_CACHE_SIZE,
      FyrehoseUtil.used_mem
    ))

    last_count = Fyrehose.backbone.msg_total.get
    last_time = FyrehoseUtil.now_ms
  }

}
