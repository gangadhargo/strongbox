package org.carlspring.strongbox.providers.repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.carlspring.strongbox.data.criteria.DetachQueryTemplate;
import org.carlspring.strongbox.data.criteria.OQueryTemplate;
import org.carlspring.strongbox.data.criteria.Paginator;
import org.carlspring.strongbox.data.criteria.Predicate;
import org.carlspring.strongbox.data.criteria.QueryTemplate;
import org.carlspring.strongbox.data.criteria.Selector;
import org.carlspring.strongbox.domain.ArtifactEntry;
import org.carlspring.strongbox.io.ArtifactOutputStream;
import org.carlspring.strongbox.io.RepositoryInputStream;
import org.carlspring.strongbox.io.RepositoryOutputStream;
import org.carlspring.strongbox.providers.io.RepositoryFiles;
import org.carlspring.strongbox.providers.io.RepositoryPath;
import org.carlspring.strongbox.providers.io.RootRepositoryPath;
import org.carlspring.strongbox.providers.layout.LayoutProvider;
import org.carlspring.strongbox.storage.Storage;
import org.carlspring.strongbox.storage.repository.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author carlspring
 */
@Component
public class HostedRepositoryProvider extends AbstractRepositoryProvider
{

    private static final Logger logger = LoggerFactory.getLogger(HostedRepositoryProvider.class);

    private static final String ALIAS = "hosted";

    @PersistenceContext
    private EntityManager entityManager;
    
    @Override
    public String getAlias()
    {
        return ALIAS;
    }

    protected RepositoryInputStream getInputStream(RepositoryPath repositoryPath)
    {
        if (repositoryPath == null)
        {
            return null;
        }
        if (!Files.exists(repositoryPath))
        {
            logger.debug(String.format("The path [%s] does not exist!", repositoryPath));
            return null;
        }

        Repository repository = repositoryPath.getFileSystem().getRepository();
        try
        {
            String path = RepositoryFiles.stringValue(repositoryPath);
            return decorate(repository.getStorage().getId(), repository.getId(), path,
                            Files.newInputStream(repositoryPath));
        }
        catch (IOException e)
        {
            logger.error(String.format("Failed to decorate InputStream for [%s]", repositoryPath), e);
            return null;
        }
    }

    @Override
    public RepositoryOutputStream getOutputStream(RepositoryPath repositoryPath)
            throws IOException
    {
        ArtifactOutputStream aos = (ArtifactOutputStream) Files.newOutputStream(repositoryPath);
        
        return decorate(repositoryPath, aos);
    }

    @Override
    public List<Path> search(String storageId,
                             String repositoryId,
                             Predicate predicate,
                             Paginator paginator)
    {
        List<Path> result = new LinkedList<Path>();

        Storage storage = configurationManager.getConfiguration().getStorage(storageId);
        Repository repository = storage.getRepository(repositoryId);
        LayoutProvider layoutProvider = layoutProviderRegistry.getProvider(repository.getLayout());
        
        Selector<ArtifactEntry> selector = createSelector(storageId, repositoryId, predicate);
        selector.setFetch(true);
        
        QueryTemplate<List<ArtifactEntry>, ArtifactEntry> queryTemplate = new DetachQueryTemplate<>(entityManager);
        
        List<ArtifactEntry> searchResult = queryTemplate.select(selector, paginator);
        for (ArtifactEntry artifactEntry : searchResult)
        {
            
            try
            {
                RootRepositoryPath rootRepositoryPath = layoutProvider.resolve(repository);
                result.add(rootRepositoryPath.resolve(artifactEntry));
            }
            catch (Exception e)
            {
                logger.error(String.format("Failed to resolve Artifact [%s]", artifactEntry.getArtifactCoordinates()),
                             e);
                continue;
            }
        }
        
        return result;
    }

    @Override
    public Long count(String storageId,
                      String repositoryId,
                      Predicate predicate)
    {
        Selector<ArtifactEntry> selector = createSelector(storageId, repositoryId, predicate).select("count(*)");

        QueryTemplate<Long, ArtifactEntry> queryTemplate = new OQueryTemplate<>(entityManager);

        return queryTemplate.select(selector);
    }

    @Override
    protected RepositoryPath fetchPath(RepositoryPath repositoryPath) 
           throws IOException
    {
        logger.debug(" -> Checking local cache for {} ...", repositoryPath);
        if (!Files.exists(repositoryPath))
        {
            //TODO: we shouldn't return null here.
            logger.debug("The artifact {} was not found in the local cache", repositoryPath);
            return null;
        }
        
        logger.debug("The artifact {} was found in the local cache", repositoryPath);
        return repositoryPath;
    }
}
