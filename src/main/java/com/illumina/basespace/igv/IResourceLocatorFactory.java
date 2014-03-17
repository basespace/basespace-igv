package com.illumina.basespace.igv;

import java.util.List;
import java.util.UUID;

import org.broad.igv.util.ResourceLocator;

import com.illumina.basespace.ApiClient;
import com.illumina.basespace.entity.File;
import com.illumina.basespace.entity.FileCompact;

public interface IResourceLocatorFactory<T extends ResourceLocator>
{
    public T newLocator(UUID clientId,ApiClient client,File check, List<FileCompact> filesInDirectory);

}
