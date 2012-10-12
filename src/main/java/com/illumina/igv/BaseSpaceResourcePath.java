package com.illumina.igv;

import org.w3c.dom.Element;

import com.illumina.igv.ui.AppResultNode;
import com.illumina.igv.ui.BaseSpaceTreeNode;
import com.illumina.igv.ui.FileNode;
import com.illumina.igv.ui.ProjectNode;
import com.illumina.igv.ui.UserNode;

public class BaseSpaceResourcePath
{
    public static final String ATTR_USER_ID = "userId";
    public static final String ATTR_PROJECT_ID = "projectId";
    public static final String ATTR_APPRESULT_ID = "appSessionId";
    public static final String ATTR_FILE_ID = "fileId";
    public static final String ATTR_INDEX_FILE_ID = "indexFileId";
    
    private Long userId = new Long(0);
    private Long projectId = new Long(0);
    private Long appResultId = new Long(0);
    private Long fileId = new Long(0);
    private Long indexFileId = new Long(0);
    
    public Long getUserId()
    {
        return userId;
    }
    public void setUserId(Long userId)
    {
        this.userId = userId;
    }
    public Long getProjectId()
    {
        return projectId;
    }
    public void setProjectId(Long projectId)
    {
        this.projectId = projectId;
    }
    public Long getAppResultId()
    {
        return appResultId;
    }
    public void setAppResultId(Long appResultId)
    {
        this.appResultId = appResultId;
    }
    public Long getFileId()
    {
        return fileId;
    }
    public void setFileId(Long fileId)
    {
        this.fileId = fileId;
    }
    
    public Long getIdFromNode(BaseSpaceTreeNode<?> node)
    {
        if (UserNode.class.isAssignableFrom(node.getClass()))
        {
            return this.getUserId();
        }
        if (ProjectNode.class.isAssignableFrom(node.getClass()))
        {
            return this.getProjectId();
        }
        if (AppResultNode.class.isAssignableFrom(node.getClass()))
        {
            return this.getAppResultId();
        }
        if (FileNode.class.isAssignableFrom(node.getClass()))
        {
            return this.getFileId();
        }
        throw new RuntimeException("Unsupported node for operation");
    }
    
    
    
    public Long getIndexFileId()
    {
        return indexFileId;
    }
    public void setIndexFileId(Long indexFileId)
    {
        this.indexFileId = indexFileId;
    }
    @Override
    public String toString()
    {
        return "BaseSpaceFilePath [userId=" + userId + ", projectId=" + projectId + ", appResultId=" + appResultId
                + ", fileId=" + fileId + "]";
    }
   
    public void setAttributes(Element elem)
    {
        elem.setAttribute(ATTR_USER_ID, getUserId().toString());
        elem.setAttribute(ATTR_PROJECT_ID, getProjectId().toString());
        elem.setAttribute(ATTR_APPRESULT_ID, getAppResultId().toString());
        elem.setAttribute(ATTR_FILE_ID, getFileId().toString());
        elem.setAttribute(ATTR_INDEX_FILE_ID, getIndexFileId().toString());
    }
    
    public void fromAttributes(Element elem)
    {
        this.setUserId(Long.parseLong(elem.getAttribute(ATTR_USER_ID)));
        this.setProjectId(Long.parseLong(elem.getAttribute(ATTR_PROJECT_ID)));
        this.setAppResultId(Long.parseLong(elem.getAttribute(ATTR_APPRESULT_ID)));
        this.setFileId(Long.parseLong(elem.getAttribute(ATTR_FILE_ID)));
        this.setIndexFileId(Long.parseLong(elem.getAttribute(ATTR_INDEX_FILE_ID)));
    }
    
}
