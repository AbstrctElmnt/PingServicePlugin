package com.bdas;

import com.atlassian.configurable.ObjectConfiguration;
import com.atlassian.configurable.ObjectConfigurationException;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.comments.Comment;
import com.atlassian.jira.issue.comments.CommentManager;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.service.AbstractService;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.web.bean.PagerFilter;
import com.opensymphony.module.propertyset.PropertySet;

import java.util.List;

public class PingService extends AbstractService{
    private String jqlQuery, user, template = null;

    @Override
    public void init(PropertySet props) throws ObjectConfigurationException {
        super.init(props);
        if (hasProperty("JQL Query")) jqlQuery = getProperty("JQL Query");
        if (hasProperty("User")) user = getProperty("User");
        if (hasProperty("Template")) template = getProperty("Template");
    }

    public void run() {
        List<Issue> issues = parseJQL(jqlQuery);
        if (jqlQuery != null && user != null && template != null && issues != null) {
            CommentManager commentManager = ComponentAccessor.getCommentManager();

            for (Issue issue : issues) {
                Comment lastComment = commentManager.getLastComment(issue);
                if (lastComment != null && !lastComment.getBody().equals(template)) commentManager.create(issue, setAppUser(user), template, true);
                else if (lastComment == null) commentManager.create(issue, setAppUser(user), template, true);
            }

        }
    }

    private ApplicationUser setAppUser(String user) {
        ApplicationUser applicationUser = ComponentAccessor.getUserManager().getUserByKey(user);
        JiraAuthenticationContext jiraAuthenticationContext = ComponentAccessor.getJiraAuthenticationContext();
        jiraAuthenticationContext.setLoggedInUser(applicationUser);
        return jiraAuthenticationContext.getLoggedInUser();
    }

    private List<Issue> parseJQL(String JQL) {
        SearchService searchService = ComponentAccessor.getComponentOfType(SearchService.class);
        final SearchService.ParseResult parseResult = searchService.parseQuery(setAppUser(user), jqlQuery);

        if (parseResult.isValid()) {
            try
            {
                final SearchResults results = searchService.search(setAppUser(user), parseResult.getQuery(), PagerFilter.getUnlimitedFilter());
                final List<Issue> issues = results.getIssues();
                return issues;

            }
            catch (SearchException e)
            {
                log.error("Error running search", e);
            }
        } else
            {
                log.warn("Error parsing jqlQuery: " + parseResult.getErrors());

            }
        return null;
    }

    public ObjectConfiguration getObjectConfiguration() throws ObjectConfigurationException {
        return getObjectConfiguration("PingService",
                "PingService.xml", null);
    }
}
