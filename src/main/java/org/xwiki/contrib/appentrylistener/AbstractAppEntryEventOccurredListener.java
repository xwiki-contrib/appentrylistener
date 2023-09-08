/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.contrib.appentrylistener;

import javax.inject.Inject;
import javax.inject.Provider;

import org.slf4j.Logger;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.ObservationContext;
import org.xwiki.observation.event.Event;
import org.xwiki.refactoring.event.DocumentRenamedEvent;
import org.xwiki.refactoring.event.DocumentRenamingEvent;
import org.xwiki.stability.Unstable;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Abstract class to create listeners for application entry operation events: deleted, renamed, created, updated,
 * etc.<b>after they happened</b>. It will basically filter and analyze generic document events to only notify when they
 * occur on application pages and also not send duplicates (e.g. for the delete that is part of a rename, the create
 * that is part of a copy, etc.). <br>
 * 
 * @version $Id$
 * @since 1.0
 */
@Unstable
public abstract class AbstractAppEntryEventOccurredListener extends AbstractEventListener
{
    @Inject
    protected Logger logger;

    @Inject
    protected ObservationContext observationContext;

    @Inject
    protected Provider<XWikiContext> contextProvider;

    /**
     * Builds a listener with the passed name.
     * 
     * @param name the name of the listener to entry events.
     */
    public AbstractAppEntryEventOccurredListener(String name)
    {
        // only delete and rename for now
        // for devs: don't add -ing events to this superclass, this one is only made for past events, not for ongoing
        // events.
        super(name, new DocumentRenamedEvent(), new DocumentDeletedEvent());
    }

    protected XWikiContext getXWikiContext()
    {
        return contextProvider.get();
    }

    /**
     * @return the class of the application entries for which this listener will listen.
     */
    public abstract EntityReference getAppClassReference();

    /**
     * Returns true if the passed entry is an entry of the application (to react to), false otherwise. By default, this
     * function checks the objects of the page based on {@link #getAppClassReference()} but can be overridden to add
     * extra conditions (such as excluding the template document, filtering on a space, etc.).
     * 
     * @param document the document to check whether it's an app entry or not.
     * @param event the original event caught by the listener, in case it's needed for getting context information for
     *            filtering the document.
     * @return true if the passed entry is an entry of the application (to react to), false otherwise.
     */
    protected boolean isAppEntry(XWikiDocument document, Event event)
    {
        return document != null && document.getXObject(getAppClassReference()) != null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.observation.EventListener#onEvent(org.xwiki.observation.event.Event, java.lang.Object,
     *      java.lang.Object)
     */
    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        if (logger.isDebugEnabled()) {
            logger.debug("{} - Event: {}", getName(), event);
        }

        // dispatch the event to the specific operation function of this listener, if the impacted document is an app
        // document and based on the received event.
        if (event instanceof DocumentRenamedEvent) {
            DocumentRenamedEvent renameEvent = (DocumentRenamedEvent) event;
            DocumentReference eventEntryRef = renameEvent.getTargetReference();
            XWikiDocument eventDoc;
            try {
                XWikiContext context = this.getXWikiContext();
                XWiki wiki = context.getWiki();
                eventDoc = wiki.getDocument(eventEntryRef, context);
            } catch (XWikiException e) {
                logger.warn("Failed to fetch renamed entry {} to evaluate its type, exiting.", eventEntryRef);
                return;
            }
            if (isAppEntry(eventDoc, event)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("App entry rename detected for {}, calling handler function", eventDoc);
                }

                this.onAppEntryRenamed(eventDoc, renameEvent.getSourceReference(), renameEvent);
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("Rename event received for document {} which is not an entry of the app", eventDoc);
                }
            }
        } else if (event instanceof DocumentDeletedEvent) {
            // there will always be a delete as a part of a rename, as the rename is made of a delete + a create. We
            // don't want to handle those deletes as deletes but as renames.
            boolean isRename = observationContext.isIn(new DocumentRenamingEvent());
            if (isRename) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Document {} deleted event is in a rename event, not handling as a delete", source);
                }
                return;
            }

            XWikiDocument eventDoc = ((XWikiDocument) source).getOriginalDocument();

            if (isAppEntry(eventDoc, event)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("App entry delete detected for {}, calling handler function", eventDoc);
                }

                this.onAppEntryDeleted(eventDoc, (DocumentDeletedEvent) event);

            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("Delete event received for document {} which is not an entry of the app", eventDoc);
                }
            }
        }
    }

    /**
     * Function called when an entry of the application (as filtered by {@link #isAppEntry(XWikiDocument, Event)}) was
     * deleted. All deletes that happen as part of a rename will not be notified on this function, but handled by the
     * {@link #onAppEntryRenamed()} instead.
     * 
     * @param originalDocument the original document, before delete. Use only for read-only purposes.
     * @param event the original document deleted event, in case any context is needed.
     */
    public abstract void onAppEntryDeleted(XWikiDocument originalDocument, DocumentDeletedEvent event);

    /**
     * Function called when an entry of the application (as filtered by {@link #isAppEntry(XWikiDocument, Event)}) was
     * renamed.
     * 
     * @param renamedDocument the XWikiDocument that was renamed, after rename. This instance is <b>not cloned</b>, so
     *            use it for read-only purposes. Should any changes be needed on it, make a clone.
     * @param oldReference the old reference of the renamed document, before rename.
     * @param event the original document renamed event, in case any context is needed.
     */
    public abstract void onAppEntryRenamed(XWikiDocument renamedDocument, DocumentReference oldReference,
        DocumentRenamedEvent event);

}
