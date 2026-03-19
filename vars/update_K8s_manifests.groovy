#!/usr/bin/env groovy

/**
 * Update Kubernetes manifests with new image tags
 */
def call(Map config = [:]) {
    def imageTag = config.imageTag ?: error("Image tag is required")
    def manifestsPath = config.manifestsPath ?: 'kubernetes'
    def gitCredentials = config.gitCredentials ?: 'github-credentials'
    def gitUserName = config.gitUserName ?: 'Jenkins CI'
    def gitUserEmail = config.gitUserEmail ?: 'jenkins@example.com'

    echo "Updating Kubernetes manifests with image tag: ${imageTag}"

    withCredentials([usernamePassword(
        credentialsId: gitCredentials,
        usernameVariable: 'GIT_USERNAME',
        passwordVariable: 'GIT_PASSWORD'
    )]) {
        // Configure Git
        sh """
            git config user.name "${gitUserName}"
            git config user.email "${gitUserEmail}"
        """

        // Update deployment manifests with new image tags - using proper Linux sed syntax
        sh """
            # Update main application deployment
            # Yahan humne file ka naam 08-qbshop-deployment.yaml kar diya hai
            sed -i "s|image: .*|image: ${env.DOCKER_IMAGE_NAME}:${imageTag}|g" ${manifestsPath}/08-qbshop-deployment.yaml

            # Update migration job
            # Yahan humne file ka naam 12-migration-job.yaml kar diya hai
            if [ -f "${manifestsPath}/12-migration-job.yaml" ]; then
                sed -i "s|image: .*|image: ${env.DOCKER_MIGRATION_IMAGE_NAME}:${imageTag}|g" ${manifestsPath}/12-migration-job.yaml
            fi
            
            # (Optional) Agar aap ingress use kar rahe hain, toh usko update karne ka code. 
            # Abhi ke liye hum domain update wala hissa hata rahe hain taaki pipeline fail na ho.
        """

        sh """
            # Check for changes
            if git diff --quiet; then
                echo "No changes to commit"
            else
                # Commit and push changes
                git add ${manifestsPath}/*.yaml
                git commit -m "Update image tags to ${imageTag} [ci skip]"

                # Set up credentials for push - YAHAN APNA REPO URL ZAROOR CHECK KAREIN
                git remote set-url origin https://\${GIT_USERNAME}:\${GIT_PASSWORD}@github.com/singhajay210208/Qualibytes-Ecommerce-Project.git
                git push origin HEAD:\${GIT_BRANCH}
            fi
        """
    }
}
