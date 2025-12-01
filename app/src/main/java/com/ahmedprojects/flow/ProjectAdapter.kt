package com.ahmedprojects.flow

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import de.hdodenhof.circleimageview.CircleImageView

class ProjectAdapter(
    private val projectList: List<Project>,
    private val onProjectClick: (Project) -> Unit
) : RecyclerView.Adapter<ProjectAdapter.ProjectViewHolder>() {

    class ProjectViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val orgLogo: CircleImageView = view.findViewById(R.id.orgLogo)
        val orgName: TextView = view.findViewById(R.id.orgName)
        val orgRoleBadge: TextView = view.findViewById(R.id.orgRoleBadge)
        val orgDescription: TextView = view.findViewById(R.id.orgDescription)
        val orgMembers: TextView = view.findViewById(R.id.orgMembers)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProjectViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.org_list_item, parent, false)
        return ProjectViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProjectViewHolder, position: Int) {
        val project = projectList[position]

        holder.orgName.text = project.name
        holder.orgRoleBadge.text = project.role.capitalize()
        holder.orgDescription.text = project.description
        holder.orgMembers.text = project.membersCount.toString()

        // Decode and display project image if available
        if (!project.pictureUrl.isNullOrEmpty()) {
            try {
                val decodedBytes = Base64.decode(project.pictureUrl, Base64.NO_WRAP)
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                holder.orgLogo.setImageBitmap(bitmap)
            } catch (e: Exception) {
                // fallback image if decoding fails
                holder.orgLogo.setImageResource(R.drawable.org_logo_placeholder)
            }
        } else {
            // default placeholder
            holder.orgLogo.setImageResource(R.drawable.org_logo_placeholder)
        }

        // CLICK EVENT
        holder.itemView.setOnClickListener {
            onProjectClick(project)
        }
    }

    override fun getItemCount(): Int = projectList.size
}
