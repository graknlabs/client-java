/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

using System.Collections.Generic;
using System.IO;
using System.Text;

namespace TypeDB.Driver.Test.TestRunner
{
    // Xunit.Gherkin.Quick cannot filter scenarios by tag, so scenarios tagged as ignored for this
    // driver are stripped from the shared feature files before test discovery reads them. Feature
    // files needing filtering are rewritten next to the test binary, and their tests reference the
    // rewritten copy in [FeatureFile].
    public static class FeatureFileFilter
    {
        private const string IgnoreTag = "@ignore-typedb-driver-csharp";
        private const string ClusterIgnoreTag = "@ignore-typedb-driver-cluster";

        // Defined by the cluster test rule, so that cluster-only exclusions apply to that run alone.
#if TYPEDB_CLUSTER_MODE
        private const bool IsClusterMode = true;
#else
        private const bool IsClusterMode = false;
#endif

        private static readonly Dictionary<string, string> FilteredFeatures = new Dictionary<string, string>
        {
            { "external/typedb_behaviour+/driver/connection.feature", "connection.csharp.feature" },
            { "external/typedb_behaviour+/driver/migration.feature", "migration.csharp.feature" },
        };

        public static void PrepareFeatureFiles()
        {
            foreach (var (source, target) in FilteredFeatures)
            {
                if (File.Exists(source))
                {
                    File.WriteAllText(target, Filter(File.ReadAllLines(source)));
                }
            }
        }

        private static string Filter(string[] sourceLines)
        {
            var filtered = new StringBuilder();
            var block = new StringBuilder();
            var previousLineIsTag = false;
            foreach (var line in sourceLines)
            {
                var trimmed = line.TrimStart();
                var indent = line.Length - trimmed.Length;
                var isScenarioHeader = indent == 2 && (trimmed.StartsWith("@") || trimmed.StartsWith("Scenario"));
                if (isScenarioHeader && !previousLineIsTag)
                {
                    FlushBlock(filtered, block);
                }
                block.AppendLine(line);
                previousLineIsTag = indent == 2 && trimmed.StartsWith("@");
            }
            FlushBlock(filtered, block);
            return filtered.ToString();
        }

        private static void FlushBlock(StringBuilder filtered, StringBuilder block)
        {
            var content = block.ToString();
            if (!content.Contains(IgnoreTag) && !(IsClusterMode && content.Contains(ClusterIgnoreTag)))
            {
                filtered.Append(content);
            }
            block.Clear();
        }
    }
}
