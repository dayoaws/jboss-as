/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package org.jboss.as.ejb3.deployment.processors.merging;

import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ee.component.EEModuleClassDescription;
import org.jboss.as.ee.metadata.ClassAnnotationInformation;
import org.jboss.as.ejb3.EjbMessages;
import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.ejb3.component.entity.EntityBeanComponentDescription;
import org.jboss.as.ejb3.component.messagedriven.MessageDrivenComponentDescription;
import org.jboss.as.ejb3.component.session.ClusteringInfo;
import org.jboss.as.ejb3.component.session.SessionBeanComponentDescription;
import org.jboss.as.ejb3.component.singleton.SingletonComponentDescription;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.ejb3.annotation.Clustered;

/**
 * @author paul
 * @author Jaikiran Pai
 */
public class ClusteredMergingProcessor extends AbstractMergingProcessor<EJBComponentDescription> {

    public ClusteredMergingProcessor() {
        super(EJBComponentDescription.class);
    }

    @Override
    protected void handleAnnotations(DeploymentUnit deploymentUnit, EEApplicationClasses applicationClasses,
                                     DeploymentReflectionIndex deploymentReflectionIndex, Class<?> componentClass,
                                     EJBComponentDescription ejbComponentDescription) throws DeploymentUnitProcessingException {
        final EEModuleClassDescription clazz = applicationClasses.getClassByName(componentClass.getName());
        //we only care about annotations on the bean class itself
        if (clazz == null) {
            return;
        }
        final ClassAnnotationInformation<Clustered, ClusteringInfo> clustering = clazz.getAnnotationInformation(Clustered.class);
        if (clustering == null || clustering.getClassLevelAnnotations().isEmpty()) {
            return;
        }
        // https://issues.jboss.org/browse/AS7-2887 Disallow @Clustered on certain EJB types
        if (ejbComponentDescription instanceof MessageDrivenComponentDescription) {
            throw EjbMessages.MESSAGES.clusteredAnnotationIsNotApplicableForMDB(deploymentUnit, ejbComponentDescription.getComponentName(), ejbComponentDescription.getComponentClassName());
        }
        if (ejbComponentDescription instanceof EntityBeanComponentDescription) {
            throw EjbMessages.MESSAGES.clusteredAnnotationIsNotApplicableForEntityBean(deploymentUnit, ejbComponentDescription.getComponentName(), ejbComponentDescription.getComponentClassName());
        }
        if (ejbComponentDescription instanceof SingletonComponentDescription) {
            throw EjbMessages.MESSAGES.clusteredAnnotationNotYetImplementedForSingletonBean(deploymentUnit, ejbComponentDescription.getComponentName(), ejbComponentDescription.getComponentClassName());
        }
        // make sure it's a session bean
        if (!(ejbComponentDescription instanceof SessionBeanComponentDescription)) {
            throw EjbMessages.MESSAGES.clusteredAnnotationIsNotApplicableForBean(deploymentUnit, ejbComponentDescription.getComponentName(), ejbComponentDescription.getComponentClassName());
        }

        // set the annotation information on the session bean description
        final ClusteringInfo clusteringInfo = clustering.getClassLevelAnnotations().get(0);
        ((SessionBeanComponentDescription) ejbComponentDescription).setClustering(clusteringInfo);
    }

    @Override
    protected void handleDeploymentDescriptor(DeploymentUnit deploymentUnit,
                                              DeploymentReflectionIndex deploymentReflectionIndex, Class<?> componentClass,
                                              EJBComponentDescription ejbComponentDescription) throws DeploymentUnitProcessingException {
    }
}
