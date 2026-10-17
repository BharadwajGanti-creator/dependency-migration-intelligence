package com.dmi.fixtures;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.function.Predicate;

public final class FixtureBounds {

    private FixtureBounds() {}

    public static int relativeDepth(Path fixtureRoot, Path file) {
        return fixtureRoot.relativize(file.normalize()).getNameCount();
    }

    public static boolean hasMatchingFileBeyondDepth(Path fixtureRoot, int maxDepth, Predicate<Path> include)
            throws IOException {
        Path root = fixtureRoot.toAbsolutePath().normalize();
        var found = new boolean[] {false};
        Files.walkFileTree(root, java.util.EnumSet.noneOf(java.nio.file.FileVisitOption.class), Integer.MAX_VALUE,
                new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        if (Files.isSymbolicLink(file) || !attrs.isRegularFile()) {
                            return FileVisitResult.CONTINUE;
                        }
                        if (include.test(file) && relativeDepth(root, file) > maxDepth) {
                            found[0] = true;
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
        return found[0];
    }
}
