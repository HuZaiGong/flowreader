package com.flowreader.app.provider

import org.junit.Assert.assertEquals
import org.junit.Test

class FlowReaderContentProviderTest {

    @Test
    fun bookCollectionRoute() {
        assertEquals(1, FlowReaderContentProvider.matchPath(FlowReaderContentProvider.PATH_BOOKS))
    }

    @Test
    fun bookByIdRoute() {
        assertEquals(2, FlowReaderContentProvider.matchPath("books/42"))
    }

    @Test
    fun progressRoute() {
        assertEquals(3, FlowReaderContentProvider.matchPath("progress/7"))
    }

    @Test
    fun unknownRoutesAreRejected() {
        assertEquals(-1, FlowReaderContentProvider.matchPath("chapters"))
        assertEquals(-1, FlowReaderContentProvider.matchPath("books/abc"))
        assertEquals(-1, FlowReaderContentProvider.matchPath(""))
    }
}
