/*
 ** Copyright (C) 2013 Mellanox Technologies
 **
 ** Licensed under the Apache License, Version 2.0 (the "License");
 ** you may not use this file except in compliance with the License.
 ** You may obtain a copy of the License at:
 **
 ** http://www.apache.org/licenses/LICENSE-2.0
 **
 ** Unless required by applicable law or agreed to in writing, software
 ** distributed under the License is distributed on an "AS IS" BASIS,
 ** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 ** either express or implied. See the License for the specific language
 ** governing permissions and  limitations under the License.
 **
 */

package com.mellanox.jxio.tests.benchmarks;

import java.net.URI;
import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.mellanox.jxio.ClientSession;
import com.mellanox.jxio.EventName;
import com.mellanox.jxio.EventQueueHandler;
import com.mellanox.jxio.EventReason;
import com.mellanox.jxio.Msg;
import com.mellanox.jxio.MsgPool;

public class ClientWorker implements Callable<double[]> {
	private final ClientSession cs;
	private final EventQueueHandler eqh;
	private final MsgPool pool;
	private final int msgSize;
	// calculation members
	private boolean firstTime;
	private long startTime;
	private long cnt;
	private int sample_cnt;
	private int res_array_index = 0;

	// results array (in the form of [tps1,bw1,tp2,bw2...]
	public double[] results;

	// logger
	private static Log LOG = LogFactory.getLog(ClientWorker.class.getCanonicalName());

	// number of messages to be sent at first burst
	int num_of_messages = 50;

	// cTor
	public ClientWorker(int inMsg_size, int outMsg_size, URI uri, int num_of_buffers, double[] res) {
		eqh = new EventQueueHandler();
		pool = new MsgPool(num_of_buffers, inMsg_size, outMsg_size);
		results = res;
		cs = new ClientSession(eqh, uri, new ClientWorkerCallbacks());
		msgSize = (inMsg_size > outMsg_size) ? inMsg_size : outMsg_size;
		int msgKSize = msgSize / 1024;
		if (msgKSize == 0) {
			sample_cnt = 40000;
		} else {
			sample_cnt = 40000 / msgKSize;
		}
		cnt = 0;
		firstTime = true;
	}

	public void close() {
		LOG.debug("closing client session");
		cs.close();
	}

	public double[] call() {
		for (int i = 0; i < num_of_messages; i++) {
			Msg msg = pool.getMsg();
			if (msg == null) {
				LOG.error("Cannot get new message");
				break;
			}
			msg.getOut().position(msg.getOut().capacity()); // simulate 'out_msgSize' was written into buffer
			if (!cs.sendMessage(msg)) {
				LOG.error("Error sending");
				pool.releaseMsg(msg);
			}
		}
		eqh.run();
		eqh.close();
		LOG.debug("deleting message pool");
		pool.deleteMsgPool();
		return results;
	}

	class ClientWorkerCallbacks implements ClientSession.Callbacks {

		public void onMsgError() {
			LOG.error("On Message Error");
		}

		public void onSessionEstablished() {
			LOG.debug("Session established");
		}

		public void onSessionEvent(EventName session_event, EventReason reason) {
			if (session_event == EventName.SESSION_TEARDOWN) {
				LOG.debug("closing eqh");
				eqh.stop();
			}
		}

		public void onReply(Msg msg) {
			if (firstTime) {
				startTime = System.nanoTime();
				firstTime = false;
			}

			cnt++;
			if (cnt == sample_cnt) {
				long delta = System.nanoTime() - startTime;
				// multiply by 10^9 because result is in seconds
				long pps = (cnt * 1000000000) / delta;
				// divide by (1024*1024) in order to get BW in MB
				double bw = (1.0 * pps * msgSize / (1024 * 1024));
				results[res_array_index] = pps;
				results[res_array_index + 1] = bw;
				res_array_index += 2;
				if (res_array_index == results.length) {
					ClientWorker.this.close();
					return;
				}
				cnt = 0;
				startTime = System.nanoTime();
			}
			if (!ClientWorker.this.cs.getIsClosing()) {
				if (!cs.sendMessage(msg)) {
					pool.releaseMsg(msg);
				}
			}
		}
	}
}
