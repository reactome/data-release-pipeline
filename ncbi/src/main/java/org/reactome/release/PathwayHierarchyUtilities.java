package org.reactome.release;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;

import java.util.*;
import java.util.stream.Collectors;

public class PathwayHierarchyUtilities {
	private static final Logger logger = LogManager.getLogger();

	private static Map<Session, Map<Long, Set<Long>>> rleToPathwayIdCache = new HashMap<>();
	private static Map<Session, Map<Long, Set<Long>>> pathwayHierarchyCache = new HashMap<>();
	private static Map<Session, Set<Long>> topLevelPathwayIdsCache = new HashMap<>();

	public static Map<Long, Set<Long>> fetchRLEIdToPathwayId(Session graphDBSession) {
		if (rleToPathwayIdCache.containsKey(graphDBSession)) {
			return rleToPathwayIdCache.get(graphDBSession);
		}

		if (graphDBSession == null) {
			throw new IllegalStateException("Neo4j driver session parameter is null");
		}

		logger.info("Computing RLE id to Pathway id");

		StatementResult statementResult = graphDBSession.run(
			String.join(System.lineSeparator(),
				"MATCH (rle:ReactionLikeEvent)<-[:hasEvent*]-(p:Pathway)",
				"RETURN DISTINCT rle.dbId, p.dbId"
			)
		);

		Map<Long, Set<Long>> rleToPathwayId = new HashMap<>();
		while (statementResult.hasNext()) {
			Record record = statementResult.next();

			long reactionLikeEventId = record.get("rle.dbId").asLong();
			long pathwayId = record.get("p.dbId").asLong();

			rleToPathwayId
				.computeIfAbsent(reactionLikeEventId, k -> new HashSet<>())
				.add(pathwayId);
		}

		rleToPathwayIdCache.put(graphDBSession, rleToPathwayId);

		logger.info("Finished computing RLE id to Pathway id");

		return rleToPathwayId;
	}

	public static Map<Long, Set<Long>> fetchPathwayHierarchy(Session graphDBSession) {
		if (pathwayHierarchyCache.containsKey(graphDBSession)) {
			return pathwayHierarchyCache.get(graphDBSession);
		}

		if (graphDBSession == null) {
			throw new IllegalStateException("Neo4j driver session parameter is null");
		}

		logger.info("Computing Pathway Hierarchy");

		StatementResult statementResult = graphDBSession.run(
			String.join(System.lineSeparator(),
				"MATCH (p:Pathway)<-[:hasEvent]-(pp:Pathway)",
				"RETURN DISTINCT p.dbId, pp.dbId"
			)
		);

		Map<Long, Set<Long>> pathwayHierarchy = new HashMap<>();
		while (statementResult.hasNext()) {
			Record record = statementResult.next();

			long pathwayId = record.get("p.dbId").asLong();
			long parentPathwayId = record.get("pp.dbId").asLong();

			Set<Long> parentPathwayIds = pathwayHierarchy.computeIfAbsent(pathwayId, k -> new HashSet<>());
			parentPathwayIds.add(parentPathwayId);
		}

		pathwayHierarchyCache.put(graphDBSession, pathwayHierarchy);

		logger.info("Finished computing Pathway Hierarchy");

		return pathwayHierarchy;
	}

	public static Set<Long> getTopLevelPathwayIds(Session graphDBSession) {
		if (topLevelPathwayIdsCache.containsKey(graphDBSession)) {
			return topLevelPathwayIdsCache.get(graphDBSession);
		}

		logger.info("Computing Top Level Pathway ids");

		Set<Long> topLevelPathwayIds = graphDBSession.run(
			String.join(System.lineSeparator(),
				"MATCH (p:TopLevelPathway)",
				"RETURN p.dbId"
			)
		)
		.stream()
		.map(record -> record.get("p.dbId").asLong())
		.collect(Collectors.toSet());

		topLevelPathwayIdsCache.put(graphDBSession, topLevelPathwayIds);

		logger.info("Finished computing Top Level Pathway ids");

		return topLevelPathwayIds;
	}

	public static Set<Long> findTopLevelPathwayIds(long pathwayId, Map<Long, Set<Long>> pathwayHierarchy) {
		checkPathwayIdAndHiearchyAreValid(pathwayId, pathwayHierarchy);

		Set<Long> topLevelPathways = new HashSet<>();

		Set<Long> parentPathwayIds = pathwayHierarchy.get(pathwayId);
		if (parentPathwayIds == null || parentPathwayIds.isEmpty()) {
			topLevelPathways.add(pathwayId);
			return topLevelPathways;
		} else {
			for (Long parentPathwayId :  parentPathwayIds) {
				topLevelPathways.addAll(findTopLevelPathwayIds(parentPathwayId, pathwayHierarchy));
			}
		}

		return topLevelPathways;
	}

	private static void checkPathwayIdAndHiearchyAreValid(long pathwayId, Map<Long, Set<Long>> pathwayHierarchy) {
		// A pathway hierarchy must be provided to find a top level pathway
		if (pathwayHierarchy == null || pathwayHierarchy.isEmpty()) {
			throw new IllegalStateException("Pathway Hierarchy has no values");
		}

		// The pathway id is invalid if does not exist in the hierarchy
		if (!existsInHierarchy(pathwayId, pathwayHierarchy)) {
			logger.warn("Pathway id " + pathwayId + " does not exist in the provided Pathway Hierarchy");
		}
	}

	private static boolean existsInHierarchy(long pathwayId, Map<Long, Set<Long>> pathwayHierarchy) {
		return pathwayHierarchy.containsKey(pathwayId) ||
			pathwayHierarchy
			.keySet()
			.stream()
			.flatMap(id -> pathwayHierarchy.get(id).stream())
			.collect(Collectors.toList())
			.contains(pathwayId);
	}
}