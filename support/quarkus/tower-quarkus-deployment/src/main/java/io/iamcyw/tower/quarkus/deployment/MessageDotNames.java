package io.iamcyw.tower.quarkus.deployment;


import io.iamcyw.tower.messaging.CommandHandle;
import io.iamcyw.tower.messaging.Message;
import io.iamcyw.tower.messaging.MetaData;
import org.jboss.jandex.DotName;

public interface MessageDotNames {

    DotName COMMANDHANDLER = DotName.createSimple(CommandHandle.class.getName());

    DotName METADATA = DotName.createSimple(MetaData.class.getName());

    DotName MESSAGE = DotName.createSimple(Message.class.getName());

}
