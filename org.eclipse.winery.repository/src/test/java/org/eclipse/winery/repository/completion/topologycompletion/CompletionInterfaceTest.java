package org.eclipse.winery.repository.completion.topologycompletion;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.winery.repository.TestWithGitBackedRepository;
import org.junit.Test;

import static org.junit.Assert.*;

public class CompletionInterfaceTest extends TestWithGitBackedRepository {

    @Test
    public void complete() throws GitAPIException {
        this.setRevisionTo("origin/plain");
    }
}
