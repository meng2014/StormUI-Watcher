package com.weibo.stormUI.dataLoader.module;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.base.Stopwatch;
import com.weibo.stormUI.dataLoader.impl.DataLoaderStorm;
import com.weibo.stormUI.model.BoltData;
import com.weibo.stormUI.model.SpoutData;
import com.weibo.stormUI.util.StringHelper;
import com.weibo.stormUI.util.URLConnectionHelper;

import net.sf.json.JSONObject;

public class ModuleDataLoader extends Thread {
	private Logger log = LogManager.getLogger(ModuleDataLoader.class);
	private DataLoaderStorm dataLoaderStorm;
	private String topologyId;

	public ModuleDataLoader(DataLoaderStorm dataLoaderStorm,String topologyId) {
		this.dataLoaderStorm = dataLoaderStorm;
		this.topologyId = topologyId;
	}

	public void run() {
		try {
			Stopwatch watch = Stopwatch.createStarted();
			Map<String ,Object> tmp = loadTopologyInfo(dataLoaderStorm.getSERVER_IP(),dataLoaderStorm.getSERVER_PORT(),topologyId);
			synchronized (dataLoaderStorm.getDatas()) {
				dataLoaderStorm.getDatas().put(topologyId, tmp);
			}
			log.info(topologyId + ", time = " + watch);
		} catch (IOException e) {
			log.catching(e);
		}
	}

	// 获取spout和bolts信息，map中有两条记录，key为spoutDatas和blotDatas，value都是list
	public Map<String, Object> loadTopologyInfo(String clusterIP, String port, String topologyId)
			throws IOException {
		String urlString = "http://" + clusterIP + ":" + port + "/api/v1/topology/" + topologyId;
		String result = URLConnectionHelper.URLConnection(urlString);
		// 解析spout数据
		// substringOfSpouts:Spout部分的数据。查找[]中间的部分
		String substringOfSpouts = result.substring(result.indexOf("\"spouts\"") + 9,
				result.indexOf("]", result.indexOf("spouts")));
		// 查询有多少条数据
		int countOFLeftBrace = StringHelper.getSubtringCount(substringOfSpouts, "{");
		List<SpoutData> spoutDatas = new ArrayList<SpoutData>();
		// 返回的数据中有很多条spout，逐个解析。
		for (int i = 0; i < countOFLeftBrace; i++) {
			String tmp = substringOfSpouts.substring(StringHelper.getSubtringIndex(substringOfSpouts, "{", i + 1),
					StringHelper.getSubtringIndex(substringOfSpouts, "}", i + 1) + 1);
			JSONObject jsonObject = JSONObject.fromObject(tmp);
			SpoutData spoutData = new SpoutData();
			spoutData.setExecutors(jsonObject.getString("executors"));
			spoutData.setEmitted(jsonObject.getString("emitted"));
			spoutData.setCompleteLatency(jsonObject.getString("completeLatency"));
			spoutData.setTransferred(jsonObject.getString("transferred"));
			spoutData.setAcked(jsonObject.getString("acked"));
			spoutData.setErrorPort(jsonObject.getString("errorPort"));
			spoutData.setSpoutId(jsonObject.getString("spoutId"));
			spoutData.setTasks(jsonObject.getString("tasks"));
			spoutData.setTopologyId(topologyId);
			spoutDatas.add(spoutData);
		}
		// 解析bolts数据
		// substringOfBlots部分的数据。查找[]中间的部分
		String substringOfBlots = result.substring(result.indexOf("\"bolts\"") + 9,
				result.indexOf("}]", result.indexOf("bolts")) + 2);
		// 查询数据条数
		countOFLeftBrace = StringHelper.getSubtringCount(substringOfBlots, "{");
		List<BoltData> blotDatas = new ArrayList<BoltData>();
		// 逐条解析
		for (int i = 0; i < countOFLeftBrace; i++) {
			String tmp = substringOfBlots.substring(StringHelper.getSubtringIndex(substringOfBlots, "{", i + 1),
					StringHelper.getSubtringIndex(substringOfBlots, "}", i + 1) + 1);
			JSONObject jsonObject = JSONObject.fromObject(tmp);
			BoltData blotData = new BoltData();
			blotData.setExecutors(jsonObject.getString("executors"));
			blotData.setEmitted(jsonObject.getString("emitted"));
			blotData.setBoltId(jsonObject.getString("encodedBoltId"));
			blotData.setTasks(jsonObject.getString("tasks"));
			blotData.setTransferred(jsonObject.getString("transferred"));
			blotData.setCapacity(jsonObject.getString("capacity"));
			blotData.setExecuteLatency(jsonObject.getString("executeLatency"));
			blotData.setExecuted(jsonObject.getString("executed"));
			blotData.setProcessLatency(jsonObject.getString("processLatency"));
			blotData.setAcked(jsonObject.getString("acked"));
			blotData.setFailed(jsonObject.getString("failed"));
			blotData.setTopologyId(topologyId);
			blotDatas.add(blotData);
		}
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("spoutDatas", spoutDatas);
		map.put("boltDatas", blotDatas);
		return map;
	}
}
