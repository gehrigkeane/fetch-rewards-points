package com.gehrig.fetch.points.service;

import com.gehrig.fetch.points.domain.PointEvent;
import com.gehrig.fetch.points.domain.UserPoints;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserPointsService {

	private static final Map<String, UserPoints> USER_POINTS = new ConcurrentHashMap<>();

	private UserPoints getOrCreateUser(final String name) {
		return USER_POINTS.computeIfAbsent(name, k -> new UserPoints(name));
	}

	public void addPoints(final String name, final PointEvent points) {
		this.getOrCreateUser(name).addPoints(points);
	}

	public List<PointEvent> deletePoints(final String name, final Long points) {
		return this.getOrCreateUser(name).deletePoints(points);
	}

	public List<PointEvent> getPoints(final String name) {
		return this.getOrCreateUser(name).getPoints();
	}

	private void resetUsers(){
		USER_POINTS.clear();
	}
}
