package me.digitlworld.moreinsolation;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.stream.Stream;

public class PathMethods
{
    public static String matchAndReturnTSuffix( Stream<TagKey<Item>> tags, String namespace, String... parentPath )
    {
        for( var tagKey : tags.toList() )
        {
            var location = tagKey.location();

            if(!location.getNamespace().equals(namespace))
            {
                continue;
            }

            var childPath = location.getPath().split("/");

            if( parentPath.length != childPath.length - 1 )
            {
                continue;
            }

            for(int index = 0; index < parentPath.length; index++ )
            {
                if(!Objects.equals(parentPath[index], childPath[index]))
                {
                    return null;
                }
            }

            return childPath[childPath.length - 1];
        }

        return null;
    }
}
