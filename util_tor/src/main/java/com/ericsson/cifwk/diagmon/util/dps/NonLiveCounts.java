package com.ericsson.cifwk.diagmon.util.dps;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.LockModeType;
import javax.persistence.Persistence;
import javax.persistence.Query;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.versant.jpa.CursorIterator;
import com.versant.jpa.VersantQuery;
import com.versant.jpa.generic.DatabaseObject;

public class NonLiveCounts {
    private static final org.apache.logging.log4j.Logger m_Log = org.apache.logging.log4j.LogManager
        .getLogger(NonLiveCounts.class);

    private static final String PERSISTENCE_UNIT_NAME_GENERIC = "dps_genericAccess";

    private void byBucket(final EntityManager em) {
        final List<String> bucketNames = getBucketNames(em);

        for ( String bucketName : bucketNames ) {
            final EntityTransaction transaction = em.getTransaction();
            transaction.begin();

            final String queryStr = "SELECT m.type,m.mibInfo.namespace FROM ManagedObjectEntity m WHERE m.bucketName = '" + bucketName + "'";
            final Query query = em.createQuery(queryStr);
            query.setLockMode(LockModeType.NONE);
            
            m_Log.debug("byBucket: " + bucketName + " Starting query");
            final CursorIterator<Object> cursor = query.unwrap(VersantQuery.class).getResultCursor(1000);            
            m_Log.debug("byBucket: " + bucketName + " Completed query");
            final Map<Object,Integer> counts = new HashMap<Object,Integer>();
            while ( cursor.hasNext() ) {
            	final Object resultObj = cursor.next();
                int typeCount = 1;
                final Object type = ((Object[])resultObj)[0];
                final Object nameSpace = ((Object[])resultObj)[1];
                final String key = nameSpace + "." + type;
                final Integer existingCount = counts.get(key);
                if ( existingCount != null ) {
                    typeCount += existingCount;
                }
                counts.put(key, typeCount);
            }

            transaction.commit();
            m_Log.debug("byBucket: Completed transaction for " + bucketName);
            
            System.out.println("BEGIN " + bucketName);
            for ( Map.Entry<Object,Integer> count : counts.entrySet() ) {
                System.out.println(count.getKey() + ":" + count.getValue());
            }
        }
    }

    public void execute(final String mode) {
        try {
            final EntityManagerFactory emf = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME_GENERIC);
            final EntityManager em = emf.createEntityManager();

            m_Log.debug("execute: emf created");

            if ( mode == null || mode.equals("projectionBucket")) {
                byBucket(em);
            } else if ( mode.equals("projection")) {
                oneQuery(em);
            } else if ( mode.equals("genericBucket")) {
                genericBucket(em);
            }

            em.close();
            emf.close();
        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }

    private void genericBucket(final EntityManager em) {
        final List<String> bucketNames = getBucketNames(em);

        for ( String bucketName : bucketNames ) {
            final EntityTransaction transaction = em.getTransaction();
            transaction.begin();

            final String queryStr = "SELECT m FROM ManagedObjectEntity m WHERE m.bucketName = '" + bucketName + "'";
            final Query query = em.createQuery(queryStr,DatabaseObject.class);
            query.setLockMode(LockModeType.NONE);

            m_Log.debug("genericBucket: " + bucketName + " Starting query");
            final List<DatabaseObject> results = query.getResultList();
            m_Log.debug("genericBucket: " + bucketName + " Completed query");
            final Map<Object,Integer> counts = new HashMap<Object,Integer>();
            for (DatabaseObject resultObj : results ) {
                final String className = resultObj.getType().getFullyQualifiedName();
                int typeCount = 1;
                final Integer existingCount = counts.get(className);
                if ( existingCount != null ) {
                    typeCount += existingCount;
                }
                counts.put(className, typeCount);
            }
            transaction.commit();

            m_Log.debug("genericBucket: " + bucketName + " Completed transaction");

            System.out.println("BEGIN " + bucketName);
            for ( Map.Entry<Object,Integer> count : counts.entrySet() ) {
                System.out.println(count.getKey() + ":" + count.getValue());
            }
        }
    }

    private void oneQuery(final EntityManager em) {
        final EntityTransaction transaction = em.getTransaction();
        transaction.begin();
        final String queryStr = "SELECT m.bucketName,m.type FROM ManagedObjectEntity m WHERE m.bucketName <> 'Live'";
        final Query query = em.createQuery(queryStr);
        query.setLockMode(LockModeType.NONE);

        final Map<Object,Map> countsByBucket = new HashMap<Object,Map>();

        m_Log.debug("oneQuery: Starting query");
        final List<Object> results = query.getResultList();
        m_Log.debug("oneQuery: Query returned");
        for (Object resultObj : results ) {
            final Object result[] = (Object[])resultObj;
            Map<Object,Integer> counts = (Map<Object,Integer>)countsByBucket.get(result[0]);
            if ( counts == null ) {
                counts = new HashMap<Object,Integer>();
                countsByBucket.put(result[0], counts);
            }
            int typeCount = 1;
            final Integer existingCount = counts.get(result[1]);
            if ( existingCount != null ) {
                typeCount += existingCount;
            }
            counts.put(result[1], typeCount);
        }

        transaction.commit();
        m_Log.debug("oneQuery: Tranaction committed");

        for ( Map.Entry<Object,Map> bucket : countsByBucket.entrySet() ) {
            System.out.println("BEGIN " + bucket.getKey());
            for ( Map.Entry<Object,Integer> count : ((Map<Object,Integer>)bucket.getValue()).entrySet() ) {
                System.out.println(count.getKey() + ":" + count.getValue());
            }
        }
    }

    private List<String> getBucketNames(final EntityManager em) {
        final List<String> bucketNames = new LinkedList<String>();

        final EntityTransaction transaction = em.getTransaction();
        transaction.begin();

        final String queryStr = "SELECT d.name FROM DataBucketEntity d WHERE d.name <> 'Live'";
        final Query query = em.createQuery(queryStr);
        query.setLockMode(LockModeType.NONE);
        for (Object resultObj : query.getResultList() ) {
            bucketNames.add((String)resultObj);
        }

        transaction.commit();

        return bucketNames;
    }

    public static void main(final String args[]) {
        com.ericsson.cifwk.diagmon.util.common.Logging.init();

        final Options options = new Options();
        options.addOption("mode",true,"Query Mode");
        options.addOption("loglevel",true,"Log Level");
        final CommandLineParser parser = new GnuParser();
        try {
            final CommandLine cmdLine = parser.parse( options, args );

            if (cmdLine.hasOption("loglevel")) {
                com.ericsson.cifwk.diagmon.util.common.Logging.setLevel(cmdLine.getOptionValue("loglevel"));
            }

            new NonLiveCounts().execute(cmdLine.getOptionValue("mode"));
        } catch (ParseException e) {
            System.out.println("ERROR: Failed to parse args " + e.getMessage());
        }
    }
}

